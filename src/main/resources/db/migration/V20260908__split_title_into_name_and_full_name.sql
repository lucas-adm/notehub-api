CREATE
EXTENSION IF NOT EXISTS unaccent;

ALTER TABLE notes
    ADD COLUMN name VARCHAR(255);
ALTER TABLE notes
    ADD COLUMN full_name VARCHAR(255);

WITH ranked AS (SELECT id,
                       trim(both '-' from regexp_replace(lower(unaccent(title)), '[^a-z0-9]+', '-', 'g')) AS base_slug,
                       row_number()                                                                          OVER (
            PARTITION BY COALESCE(user_id::text, id::text),
                         trim(both '-' from regexp_replace(lower(unaccent(title)), '[^a-z0-9]+', '-', 'g'))
            ORDER BY created_at
        ) AS rn
                FROM notes)
UPDATE notes n
SET name = CASE WHEN r.rn = 1 THEN r.base_slug ELSE r.base_slug || '-' || r.rn END FROM ranked r
WHERE n.id = r.id;

UPDATE notes n
SET full_name = u.username || '/' || n.name FROM users u
WHERE n.user_id = u.id;

UPDATE notes
SET full_name = substring(id::text, 1, 8) || '/' || name
WHERE user_id IS NULL
  AND full_name IS NULL;

ALTER TABLE notes
    ALTER COLUMN name SET NOT NULL;
ALTER TABLE notes
    ALTER COLUMN full_name SET NOT NULL;
ALTER TABLE notes
    ADD CONSTRAINT uq_notes_user_name UNIQUE (user_id, name);