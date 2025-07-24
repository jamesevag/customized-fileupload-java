CREATE TABLE public.zip_entry_metadata (
                                           id int8 NOT NULL,
                                           compressed_size int8 NOT NULL,
                                           directory bool NOT NULL,
                                           "path" varchar(255) NULL,
                                           "size" int8 NOT NULL,
                                           upload_session_id uuid NULL,
                                           CONSTRAINT zip_entry_metadata_pkey PRIMARY KEY (id),
                                           CONSTRAINT fkh30ywy1gerts02e2k2sjka7fy FOREIGN KEY (upload_session_id) REFERENCES public.upload_session(id)
);