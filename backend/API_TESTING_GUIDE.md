# 🎮 PvP Coding Platform - API Testing Guide

This guide explains how to test the backend APIs using **Postman**.

## 🚀 Base Configuration
- **Base URL:** `http://localhost:8081`
- **Port:** `8081` (Changed from 8080 to avoid local conflicts)
- **Authentication:** JWT Bearer Token

---

## 1. Authentication Flow

### A. Signup (New User)
- **Method:** `POST`
- **URL:** `{{baseUrl}}/auth/signup`
- **Body (JSON):**
```json
{
    "username": "player1",
    "email": "player1@example.com",
    "password": "password123"
}
```

### B. Login
- **Method:** `POST`
- **URL:** `{{baseUrl}}/auth/login`
- **Body (JSON):**
```json
{
    "username": "player1",
    "password": "password123"
}
```
> **IMPORTANT:** Copy the `token` from the response. You must use it in the **Authorization** header for all other requests.

---

## 2. Matchmaking & Execution

### A. Join Matchmaking Queue
- **Method:** `GET`
- **URL:** `{{baseUrl}}/match/find`
- **Auth:** Bearer Token
- **Description:** Adds you to the Redis-based queue. Matchmaking logic will run in the background to pair you with another player.

### B. Submit Code
- **Method:** `POST`
- **URL:** `{{baseUrl}}/match/submit`
- **Auth:** Bearer Token
- **Body (JSON):**
```json
{
    "matchId": 1,
    "code": "public class Main { public static void main(String[] args) { System.out.println(\"Hello World\"); } }",
    "language": "java"
}
```
> Supported languages: `java`, `python`, `cpp`.

### C. Get Match Results
- **Method:** `GET`
- **URL:** `{{baseUrl}}/match/result/{matchId}`
- **Auth:** Bearer Token

---

## 3. Leaderboard
- **Method:** `GET`
- **URL:** `{{baseUrl}}/leaderboard`
- **Auth:** Bearer Token

---

## 💡 Postman Tips

### Automated Token Handling
To avoid manual copy-pasting of tokens:
1. Open your **Login** request in Postman.
2. Go to the **Tests** tab.
3. Add this script:
   ```javascript
   const response = pm.response.json();
   if (response.token) {
       pm.environment.set("jwt_token", response.token);
   }
   ```
4. Now, in all other requests, go to **Authorization** -> **Bearer Token** and set the value to `{{jwt_token}}`.

### Local Infrastructure Reminder
Ensure your local services are running via Docker:
```powershell
docker compose up -d
```
This starts:
- **Postgres** (Port 5433)
- **Redis** (Port 6379)
- **Judge0** (Port 2358)
