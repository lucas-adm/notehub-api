package br.com.notehub.application.implementation.user;

import br.com.notehub.domain.feed.FeedService;
import br.com.notehub.domain.follow.FollowService;
import br.com.notehub.domain.follow.events.UserDeletedEvent;
import br.com.notehub.domain.history.UserHistoryService;
import br.com.notehub.domain.note.NoteService;
import br.com.notehub.domain.token.TokenService;
import br.com.notehub.domain.user.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import static br.com.notehub.infra.exception.CustomExceptions.*;

@Component
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository repository;
    private final UserIdentityRepository userIdentityRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserHistoryService historian;
    private final FollowService followService;
    private final TokenService tokenService;
    private final NoteService noteService;
    private final PasswordEncoder encoder;
    private final FeedService feeder;

    @Value("${supabase.url}")
    private String supabaseUrl;

    private void validateGif(User user, String img, String field) {
        if (img == null) return;
        String expectedPrefix = "%s/storage/v1/object/public/images/avatars/".formatted(supabaseUrl);
        boolean isFromStorage = img.startsWith(expectedPrefix);
        boolean isAnimatedMedia = img.endsWith(".gif") || img.endsWith(".webm");
        if (!isAnimatedMedia) return;
        if (!user.isDev() && !user.isSponsor()) throw new GifNotAllowedException("avatar", "GIFs apenas para patrocinadores.");
        if (field.equals("banner")) throw new GifNotAllowedException("banner", "GIFs são proibidos como banner.");
        if (!isFromStorage) throw new GifNotAllowedException("avatar", "Mídia de avatar inválido.");
    }

    @SneakyThrows
    private <T> void changeField(UUID idFromToken, String field, Function<User, T> getter, Consumer<User> setter) {
        User user = repository.findById(idFromToken).orElseThrow(EntityNotFoundException::new);
        T oldValue = getter.apply(user);
        setter.accept(user);
        T newValue = getter.apply(user);
        if (Objects.equals(oldValue, newValue)) return;
        if (user.isBlocked() && (field.equals("avatar") || field.equals("banner"))) throw new UserBlockedException(field);
        if (field.equals("avatar") || field.equals("banner")) validateGif(user, (String) newValue, field);
        repository.save(user);
        historian.setHistory(user, field, String.valueOf(oldValue), String.valueOf(newValue));
    }

    private String validatePassword(String oldPassword, String newPassword) {
        if (encoder.matches(newPassword, oldPassword)) throw new SamePasswordException();
        return encoder.encode(newPassword);
    }

    private void validateEmail(String oldEmail, String newEmail) {
        repository.findByEmail(newEmail).ifPresent(user -> {
            if (Objects.equals(oldEmail, newEmail)) throw new SameEmailExpection();
            throw new DataIntegrityViolationException("email");
        });
    }

    private void validateUsername(UUID idFromToken, String username) {
        repository.findByUsername(username).ifPresent((stored) -> {
            repository.findById(idFromToken).ifPresent((user) -> {
                        if (!Objects.equals(stored, user)) {
                            throw new DataIntegrityViolationException("username");
                        }
                    }
            );
        });
    }

    private void validateActiveField(boolean active) {
        if (!active) throw new EntityNotFoundException();
    }

    private Subscription validateSubscription(String subscriptionStr) {
        try {
            return Subscription.from(subscriptionStr);
        } catch (IllegalArgumentException e) {
            throw new SubscriptionException("Inscrição inválida.");
        }
    }

    @Transactional
    @Override
    public User create(User user) {
        boolean existsByEmail = repository.existsByEmail(user.getEmail());
        boolean existsByUsername = repository.existsByUsername(user.getUsername());
        if (existsByEmail && existsByUsername) throw new DataIntegrityViolationException("both");
        if (existsByEmail) throw new DataIntegrityViolationException("email");
        if (existsByUsername) throw new DataIntegrityViolationException("username");
        user.setPassword(encoder.encode(user.getPassword()));
        return repository.save(user);
    }

    @Override
    public String generateActivationToken(User user) {
        return tokenService.generateActivationToken(user);
    }

    @Transactional
    @Override
    public void activate(UUID idFromToken) {
        changeField(idFromToken, "active", User::isActive, user -> user.setActive(true));
    }

    @Transactional
    @Override
    public void promote(UUID idFromToken) {
        changeField(idFromToken, "sponsor", User::isSponsor, user -> user.setSponsor(true));
    }

    @Transactional
    @Override
    public void changePassword(String email, String newPassword) {
        User entity = repository.findByEmail(email).orElseThrow(EntityNotFoundException::new);
        String password = validatePassword(entity.getPassword(), newPassword);
        changeField(entity.getId(), "password", User::getPassword, user -> user.setPassword(password));
    }

    @Transactional
    @Override
    public void changeEmail(String oldEmail, String newEmail) {
        validateEmail(oldEmail, newEmail);
        User entity = repository.findByEmail(oldEmail).orElseThrow(EntityNotFoundException::new);
        changeField(entity.getId(), "email", User::getEmail, user -> user.setEmail(newEmail.toLowerCase()));
    }

    @Transactional
    @Override
    public User edit(UUID idFromToken, User user) {
        validateUsername(idFromToken, user.getUsername());
        changeField(idFromToken, "username", User::getUsername, stored -> stored.setUsername(user.getUsername()));
        changeField(idFromToken, "display_name", User::getDisplayName, stored -> stored.setDisplayName(user.getDisplayName()));
        changeField(idFromToken, "avatar", User::getAvatar, stored -> stored.setAvatar(user.getAvatar()));
        changeField(idFromToken, "banner", User::getBanner, stored -> stored.setBanner(user.getBanner()));
        changeField(idFromToken, "message", User::getMessage, stored -> stored.setMessage(user.getMessage()));
        changeField(idFromToken, "profile_private", User::isProfilePrivate, stored -> stored.setProfilePrivate(user.isProfilePrivate()));
        return user;
    }

    @Transactional
    @Override
    public void changeProfileVisibility(UUID idFromToken) {
        changeField(idFromToken, "profile_private", User::isProfilePrivate, user -> user.setProfilePrivate(!user.isProfilePrivate()));
        User actor = repository.findById(idFromToken).orElseThrow(EntityNotFoundException::new);
        feeder.onProfilePrivacyChanged(actor.getId(), actor.isProfilePrivate());
    }

    @Transactional
    @Override
    public void changeUsername(UUID idFromToken, String username) {
        validateUsername(idFromToken, username);
        changeField(idFromToken, "username", User::getUsername, user -> user.setUsername(username.toLowerCase()));
    }

    @Transactional
    @Override
    public void changeDisplayName(UUID idFromToken, String displayName) {
        changeField(idFromToken, "display_name", User::getDisplayName, user -> user.setDisplayName(displayName));
    }

    @Transactional
    @Override
    public void changeAvatar(UUID idFromToken, String avatar) {
        changeField(idFromToken, "avatar", User::getAvatar, user -> user.setAvatar(avatar));
    }

    @Transactional
    @Override
    public void changeBanner(UUID idFromToken, String banner) {
        changeField(idFromToken, "banner", User::getBanner, user -> user.setBanner(banner));
    }

    @Transactional
    @Override
    public void changeMessage(UUID idFromToken, String message) {
        changeField(idFromToken, "message", User::getMessage, user -> user.setMessage(message));
    }

    @Transactional
    @Override
    public void allowSubscription(UUID idFromToken, String subscriptionStr) {
        Subscription subscription = validateSubscription(subscriptionStr);
        User user = repository.findById(idFromToken).orElseThrow(EntityNotFoundException::new);
        user.enable(subscription);
        repository.save(user);
    }

    @Transactional
    @Override
    public void disallowSubscription(UUID idFromToken, String subscriptionStr) {
        Subscription subscription = validateSubscription(subscriptionStr);
        User user = repository.findById(idFromToken).orElseThrow(EntityNotFoundException::new);
        user.disable(subscription);
        repository.save(user);
    }

    @Transactional
    @Override
    public void delete(UUID idFromToken, String password) {
        User user = repository.findById(idFromToken).orElseThrow(EntityNotFoundException::new);
        boolean matches = encoder.matches(password, user.getPassword());
        if (!matches) throw new BadCredentialsException("password");
        if (user.isProfilePrivate()) noteService.deleteAllUserNotes(user);
        else noteService.deleteAllUserHiddenNotes(user);
        noteService.setOrphanFullNameForUser(user.getId());
        Set<UUID> followersIds = followService.getUserFollowersId(idFromToken);
        Set<UUID> followingIds = followService.getUserFollowingId(idFromToken);
        repository.delete(user);
        eventPublisher.publishEvent(new UserDeletedEvent(followersIds, followingIds));
    }

    @Override
    public User getUser(String username) {
        User user = repository.findByUsername(username).orElseThrow(EntityNotFoundException::new);
        validateActiveField(user.isActive());
        return user;
    }

    @Override
    public List<User> getAllActiveUsers() {
        return repository.findAllByActiveTrue();
    }

    @Override
    public Page<User> findAll(Pageable pageable, String q) {
        return repository.findAllActiveUsersByUsernameOrDisplayName(pageable, q);
    }

    @Override
    public List<Host> getUserIdentities(UUID idFromToken) {
        List<UserIdentity> identities = userIdentityRepository.findAllByUserIdOrderByLinkedAtDesc(idFromToken);
        return identities.stream().map(UserIdentity::getHost).toList();
    }

    @Override
    public List<String> getUserDisplayNameHistory(String username) {
        User user = repository.findByUsername(username).orElseThrow(EntityNotFoundException::new);
        validateActiveField(user.isActive());
        return historian.getLastFiveUserDisplayName(user);
    }

    @Override
    public Set<Subscription> getUserSubscriptions(UUID idFromToken) {
        User user = repository.findById(idFromToken).orElseThrow(EntityNotFoundException::new);
        return user.getSubscriptions();
    }

    @Transactional
    @Override
    public void cleanUsersWithExpiredActivationTime() {
        Instant nowMinus7Days = Instant.now().minus(7, ChronoUnit.DAYS);
        List<User> usersWithExpiredActivationTime = repository.findUsersWithExpiredActivationTime(nowMinus7Days);
        repository.deleteAll(usersWithExpiredActivationTime);
    }

}