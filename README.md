# Lab Assignment 03 — Book API

This project implements the required PUT and DELETE REST operations from Lab Assignment-03.

## Endpoints

- GET `/api/v1/books`
- PUT `/api/v1/books/{bookId}`
- DELETE `/api/v1/books/{bookId}`

## Initial data

1. Java Programming — John Smith — 5 copies
2. Web Development — Sara Ahmad — 3 copies
3. Database Systems — Ali Khan — 4 copies

## Postman tests

### 1. GET all books
GET `http://localhost:8080/api/v1/books`

Expected: `200 OK`

### 2. Update book 2
PUT `http://localhost:8080/api/v1/books/2`

Body → raw → JSON:

```json
{
  "title": "Modern Web Development",
  "author": "Sara Ahmad",
  "availableCopies": 6
}
```

Expected: `200 OK` and the returned book keeps `id: 2`.

### 3. Delete book 3
DELETE `http://localhost:8080/api/v1/books/3`

Expected: `204 No Content`

### 4. Delete missing book
DELETE `http://localhost:8080/api/v1/books/99`

Expected: `404 Not Found`

### 5. Verify changes
GET `http://localhost:8080/api/v1/books`

Book 2 should be updated and book 3 should no longer appear.
