alter table emerald_grove.articles
    add column updated_at timestamp;

update emerald_grove.articles
set updated_at = created_at
where updated_at is null;

alter table emerald_grove.articles
    alter column updated_at set not null;

alter table emerald_grove.article_notes
    add column updated_at timestamp;

update emerald_grove.article_notes
set updated_at = created_at
where updated_at is null;

alter table emerald_grove.article_notes
    alter column updated_at set not null;
