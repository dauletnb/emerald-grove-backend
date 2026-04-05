alter table emerald_grove.article_notes
    add column client_created_at bigint;

update emerald_grove.article_notes
set client_created_at = extract(epoch from created_at) * 1000
where client_created_at is null;

alter table emerald_grove.article_notes
    alter column client_created_at set not null;
