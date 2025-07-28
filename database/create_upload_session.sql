CREATE TABLE public.upload_session (
                                       id uuid NOT NULL,
                                       file_name varchar(255) NOT NULL,
                                       total_size int8 NOT NULL,
                                       uploaded_size int8 NOT NULL,
                                       completed bool DEFAULT false NOT NULL,
                                       created_at timestamp DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                       created_by VARCHAR(50) NOT NULL,

                                       CONSTRAINT upload_session_pkey PRIMARY KEY (id)
);