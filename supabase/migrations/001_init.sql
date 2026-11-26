-- Ronix schema + Row Level Security. Run once in Supabase SQL editor (or via CLI).
create table if not exists profiles (
  id uuid primary key references auth.users on delete cascade,
  name text not null default '', email text not null default '',
  role text not null default 'student' check (role in ('student','admin')),
  avatar_url text, created_at timestamptz not null default now());

create or replace function is_admin() returns boolean language sql security definer stable set search_path = public as
$$ select exists (select 1 from profiles where id = auth.uid() and role = 'admin') $$;

create or replace function handle_new_user() returns trigger language plpgsql security definer set search_path = public as
$$ begin insert into profiles (id, email, name) values (new.id, new.email, coalesce(new.raw_user_meta_data->>'name','')); return new; end $$;
drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created after insert on auth.users for each row execute function handle_new_user();

-- a student can never change their own role (SQL editor / service role has auth.uid() = null and may)
create or replace function protect_role() returns trigger language plpgsql security definer set search_path = public as
$$ begin if new.role <> old.role and auth.uid() is not null and not is_admin() then new.role := old.role; end if; return new; end $$;
drop trigger if exists protect_role_trg on profiles;
create trigger protect_role_trg before update on profiles for each row execute function protect_role();

create table if not exists courses (
  id uuid primary key default gen_random_uuid(),
  title text not null check (length(trim(title)) > 0), description text, thumbnail_url text,
  access text not null default 'free' check (access in ('free','paid')),
  published boolean not null default false, featured boolean not null default false,
  created_at timestamptz not null default now());
create table if not exists chapters (
  id uuid primary key default gen_random_uuid(),
  course_id uuid not null references courses on delete cascade, title text not null, position int not null default 0);
create table if not exists lessons (
  id uuid primary key default gen_random_uuid(),
  chapter_id uuid not null references chapters on delete cascade,
  course_id uuid not null references courses on delete cascade, title text not null, position int not null default 0);
-- video ids live in their own table so paid videos are only readable by approved students
create table if not exists lesson_videos (
  lesson_id uuid primary key references lessons on delete cascade,
  youtube_id text not null check (youtube_id ~ '^[A-Za-z0-9_-]{11}$'));
create table if not exists enrollments (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references profiles on delete cascade,
  course_id uuid not null references courses on delete cascade,
  status text not null check (status in ('pending','approved','rejected')),
  created_at timestamptz not null default now(), unique (user_id, course_id));
create table if not exists lesson_progress (
  user_id uuid not null references profiles on delete cascade,
  lesson_id uuid not null references lessons on delete cascade,
  course_id uuid not null references courses on delete cascade,
  last_watched timestamptz not null default now(), primary key (user_id, lesson_id));
create table if not exists activity (
  user_id uuid not null references profiles on delete cascade, day date not null, primary key (user_id, day));
create table if not exists comments (
  id uuid primary key default gen_random_uuid(),
  lesson_id uuid not null references lessons on delete cascade,
  user_id uuid not null default auth.uid() references profiles on delete cascade,
  body text not null check (length(trim(body)) between 1 and 500),
  created_at timestamptz not null default now());
create index if not exists comments_lesson_idx on comments (lesson_id, created_at desc);
create table if not exists announcements (
  id uuid primary key default gen_random_uuid(), title text not null, body text not null default '',
  created_at timestamptz not null default now());

create or replace function can_watch(cid uuid) returns boolean language sql security definer stable set search_path = public as
$$ select is_admin() or exists (select 1 from enrollments e join courses c on c.id = e.course_id
   where e.course_id = cid and e.user_id = auth.uid() and e.status = 'approved' and c.published) $$;

alter table profiles enable row level security; alter table courses enable row level security;
alter table chapters enable row level security; alter table lessons enable row level security;
alter table lesson_videos enable row level security; alter table enrollments enable row level security;
alter table lesson_progress enable row level security; alter table activity enable row level security;
alter table comments enable row level security; alter table announcements enable row level security;

create policy profiles_read on profiles for select using (id = auth.uid() or is_admin());
create policy profiles_update on profiles for update using (id = auth.uid() or is_admin());

create policy courses_read on courses for select using (published or is_admin());
create policy courses_admin on courses for all using (is_admin()) with check (is_admin());
create policy chapters_read on chapters for select using (is_admin() or exists (select 1 from courses c where c.id = course_id and c.published));
create policy chapters_admin on chapters for all using (is_admin()) with check (is_admin());
create policy lessons_read on lessons for select using (is_admin() or exists (select 1 from courses c where c.id = course_id and c.published));
create policy lessons_admin on lessons for all using (is_admin()) with check (is_admin());
create policy videos_read on lesson_videos for select using (exists (select 1 from lessons l where l.id = lesson_id and can_watch(l.course_id)));
create policy videos_admin on lesson_videos for all using (is_admin()) with check (is_admin());

create policy enroll_read on enrollments for select using (user_id = auth.uid() or is_admin());
create policy enroll_insert on enrollments for insert with check (user_id = auth.uid() and (
  (status = 'approved' and exists (select 1 from courses c where c.id = course_id and c.published and c.access = 'free')) or
  (status = 'pending'  and exists (select 1 from courses c where c.id = course_id and c.published and c.access = 'paid'))));
create policy enroll_admin on enrollments for update using (is_admin()) with check (is_admin());
create policy enroll_delete on enrollments for delete using (is_admin());

create policy progress_own on lesson_progress for all using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy activity_own on activity for all using (user_id = auth.uid()) with check (user_id = auth.uid());

create policy comments_read on comments for select using (exists (select 1 from lessons l where l.id = lesson_id and can_watch(l.course_id)));
create policy comments_insert on comments for insert with check (user_id = auth.uid() and exists (select 1 from lessons l where l.id = lesson_id and can_watch(l.course_id)));
create policy comments_update on comments for update using (user_id = auth.uid()) with check (user_id = auth.uid());
create policy comments_delete on comments for delete using (user_id = auth.uid() or is_admin());

create policy ann_read on announcements for select using (auth.uid() is not null);
create policy ann_admin on announcements for all using (is_admin()) with check (is_admin());

-- Storage: public-read bucket for course thumbnails, admin-only writes
insert into storage.buckets (id, name, public) values ('thumbnails','thumbnails', true) on conflict (id) do nothing;
create policy thumbs_admin_write on storage.objects for insert with check (bucket_id = 'thumbnails' and is_admin());
create policy thumbs_admin_update on storage.objects for update using (bucket_id = 'thumbnails' and is_admin());
create policy thumbs_admin_delete on storage.objects for delete using (bucket_id = 'thumbnails' and is_admin());

-- Make a registered user the administrator (run manually in the SQL editor):
-- update profiles set role = 'admin' where email = 'OWNER_EMAIL_HERE';
