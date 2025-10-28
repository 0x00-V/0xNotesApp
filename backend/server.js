import express from "express";
import https from "https";
import axios from "axios";
import dotenv from "dotenv";
import {Client} from "pg";
import cookieParser from "cookie-parser";
import bodyParser from "body-parser";
import fs from "fs";
import fsp from "fs/promises";
import crypto from "crypto";

dotenv.config();
 
const client = new Client({
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    host: process.env.DB_HOST,
    port: process.env.DB_PORT,
    database: process.env.DB_DATABASE,
});
await client.connect();


function generateSessionId(){
  return crypto.randomBytes(32).toString("base64url");
}


const app = express();
app.use(cookieParser());
app.use(express.urlencoded({extends: true}));
app.use(bodyParser.json());



app.get("/api/helloworld", async (req, res) => {
  return res.status(200).json({text : "Welcome!"});
});

app.post("/api/login", async (req, res) => {
  const {username, password} = req.body;
  try{
    const query_1 = await client.query("SELECT id, username, password FROM users WHERE username = $1", [username]);
    if(query_1.rows.length === 0 || query_1.rows[0].password != password){
      return res.status(401).json({success: false, message: "Invalid Credentials"});
    }
      const session = generateSessionId();
      await client.query("INSERT INTO sessions (session_id, user_id, expires_at) VALUES ($1, $2, NOW() + interval '1 hour')", [session, query_1.rows[0].id]);
      res.cookie("session", session, { httpOnly: false, secure: false,sameSite: "Strict", maxAge: 3600 * 1000,});
      res.json({succes: true, message: "Logged In"});
    }catch(error){
      console.log("Query request threw an error. Retrying Database connection.\n"+error);
      res.status(402).json({success: false, message: "Backend Error."});
  }
});

app.get("/api/userdata", async (req, res) => {
  const session = req.cookies.session;
  if(!session) return res.status(401).json({success: false, message: "Not Authorised"});
  const result = await client.query("SELECT users.username, users.password FROM sessions JOIN users on sessions.user_id WHERE sessions.session = $1 AND session.expires_at > NOW()", [session]);
  if(result.rows.length === 0) return res.status(401).json({success: false, message: "Not Authorised"});
  const username = result.rows[0].username;
  res.json({succes: true, message: "Logged In As" + username});  
});


/*const options = {
  key: fs.readFileSync("certs/server.key"),
  cert: fs.readFileSync("certs/server.cert"),
};

http.createServer(options, app).listen(8000, "0.0.0.0", () => {
  console.log("HTTPS Server live at: https://0.0.0.0:8443");
});

*/ 

app.listen(8000, "0.0.0.0", () => {
  console.log("Backend is up and running!");
});


app.use(async (req, res) => {
  res.status(404).json({404 : "Not Found."});});




/*
 My Schemas:

CREATE TABLE users (
  id SERIAL PRIMARY KEY,
  username VARCHAR(255) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL
  );
 
CREATE TABLE sessions (
  session VARCHAR(255) PRIMARY KEY,
  user_id INT REFERENCES users(id),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP
);


 */
