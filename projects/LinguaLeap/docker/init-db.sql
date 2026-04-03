-- Create additional databases for microservices
-- (ll_user is created by POSTGRES_DB env var)
CREATE DATABASE ll_content;
CREATE DATABASE ll_ai;

-- Grant privileges
GRANT ALL PRIVILEGES ON DATABASE ll_user TO lingualeap;
GRANT ALL PRIVILEGES ON DATABASE ll_content TO lingualeap;
GRANT ALL PRIVILEGES ON DATABASE ll_ai TO lingualeap;
