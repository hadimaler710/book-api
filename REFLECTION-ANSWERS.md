# Reflection Questions — Books REST API Lab (LAB-01)

Course: Enterprise Web Application Development — Kabul University, Department of Information Systems
Project: book-api (`edu.ku.bookapi`)
Student: **\<Your Student ID\> — \<Your Name\>**  <!-- replace before submitting -->

---

### 1. Why is GET appropriate for /api/v1/books?

GET is the HTTP method defined for *reading/retrieving* a resource without changing
anything on the server. The operation here is "return the collection of all books",
which is a safe, read-only, idempotent operation, so GET matches it exactly. A POST
would mean "create a new book", PUT/DELETE would mean modify/remove — none of those
describe retrieving the catalogue. Also, because GET is a read, no request body is
required, which suits a simple catalogue query.

### 2. What is the purpose of @RestController?

`@RestController` marks the class as a REST controller that Spring discovers through
component scanning. It is a convenience annotation combining `@Controller` and
`@ResponseBody`: every handler method's return value is written **directly to the
HTTP response body** (converted to JSON by Jackson) instead of being interpreted as
a view name to render an HTML page. In this lab it makes `getAllBooks()` return the
`List<Books>` as a JSON array.

### 3. What does `<Books>` mean in `List<Books>`?

`<Books>` is the **generic type parameter** of the Java `List` interface. It declares
that this list may only contain `Books` objects. This gives compile-time type safety:
the compiler rejects `books.add("some string")`, and when we iterate the list each
element is already typed as `Books`, so no casting is needed. It also documents the
intended content of the collection.

### 4. Why is `books` plural in the URI, and what does `v1` represent?

In REST design, the endpoint `/api/v1/books` names a **collection resource**, so the
resource name is a plural noun ("all the books"), while a single item would use the
singular form with an identifier (`/api/v1/books/{id}`). `v1` is the **API version**.
Versioning the URI lets us publish future, breaking changes under `/api/v2/books`
while existing clients keep working against v1 unchanged.

### 5. How does a Java `List<Books>` become JSON without manually writing JSON code?

Spring MVC uses the **HttpMessageConverter** mechanism. Since `@RestController`
implies `@ResponseBody`, when `getAllBooks()` returns, Spring inspects the returned
type and the request's `Accept` header and selects
`MappingJackson2HttpMessageConverter` (Jackson is included through the
`spring-boot-starter-web` dependency). Jackson serializes each `Books` object by
reading its public getters — `getId()`, `getTitle()`, `getAuthor()`, `getIsbn()`,
`getPublishedYear()`, `getCategory()` — and turns each into a JSON property,
producing a JSON array for the list. The controller code never touches JSON itself;
it only returns Java objects.

### 6. What would happen if BookController were placed outside the package hierarchy scanned by the Spring Boot application?

`@SpringBootApplication` performs component scanning starting from the package of
the main class (`edu.ku.bookapi`) and only in its **sub-packages**. A controller
placed outside that hierarchy (for example in `edu.ku.controller`) would never be
registered as a bean, so Spring MVC would find no handler for `GET /api/v1/books`
and every request would return **404 Not Found** — even though the project compiles
without errors. The fix is to keep all classes under `edu.ku.bookapi` (this exact
problem existed in the initial project files and was corrected by moving
`BookController` into `edu.ku.bookapi.controller`).

---

*All answers verified against the running application: `GET http://localhost:8080/api/v1/books`
returned HTTP 200 OK with `Content-Type: application/json` and all five book objects.*
