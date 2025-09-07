CREATE TABLE IF NOT EXISTS face_embeddings (
  id UUID PRIMARY KEY,
  person_id VARCHAR(255) NOT NULL,
  model_name VARCHAR(255) NOT NULL,
  dimension INTEGER,
  embedding_blob BLOB NOT NULL,
  pgvector_embedding BINARY,
  oracle_embedding BLOB,
  mysql_embedding BLOB,
  image_hash VARCHAR(255),
  confidence DOUBLE,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  version BIGINT
); 