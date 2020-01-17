create table memes_bucket (
	bucket_id       varchar(200) not null primary key,
	lang            varchar(2)   not null,
	text            varchar(500),
	source          varchar(50)  not null,
	pub_date        timestamp    not null
)
/

create index idx_memes_bucket_cd on memes_bucket(pub_date, lang);
/

create index idx_memes_bucket_src on memes_bucket(source);
/

create table memes_data (
	content_id      varchar(200) not null,
	bucket_id       varchar(200) not null,
	url             varchar(100) not null,
	hash            varchar(256) not null,
	pub_date        timestamp    not null,

	constraint memes_data_pk primary key (content_id),
	constraint memes_data_fk foreign key (bucket_id) references memes_bucket(bucket_id)
)
/