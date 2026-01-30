CREATE TABLE face_vector_store (
    id SERIAL PRIMARY KEY,
    person_name TEXT,
    embedding VECTOR(512)  -- ArcFace R50이 512차원 벡터를 생성하기 때문
);