# 0xNotesApp
*The actual notes list is not functional and I'm yet to implement. If you relog you will get OOB (Out of Bounds) if you delete all elements
### Backend Setup `(/backend)`
1 - `npm install`

2 - `docker compose up -d`

You can view the schemas you need to add at the bottom of server.js.
Database creds can be seen in `docker-compose.yml`, and need to be changed in `.env`

Once you've connected to the database, add a user by typing `INSERT INTO users (username, password) VALUES ('user', 'pass');`.
I've not really cared about implementing encryption, but it's very easy using a library. You can checkout password encryption in one of my repositories called "Lockbox".

`.env` Structure:
```
DB_USER=dan
DB_PASS=backend
DB_HOST=127.0.0.1
DB_PORT=5432
DB_DATABASE=backend
```

### Application Setup
1 - `javac Main.java`

2 - `java Main`
Ensure server.js is running so you can reach the API endpoints.
