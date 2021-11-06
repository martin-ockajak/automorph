# REST-RPC

REST-RPC is an RPC protocol prescribing a standard way to model REST APIs while reusing existing practices and tools in the REST ecosystem.

## Motivation

[REST](https://en.wikipedia.org/wiki/Representational_state_transfer) as a theoretical concept is deemed independent of specific data formats, transport protocols or even calling conventions. However, in practice the vast majority REST APIs are created for web applications and services by transforming HTTP requests and responses in JSON format to remote API function calls.

Such transformations require deciding where to store various REST API call data and metadata in the underlying transport protocol and message format in the same way [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call) protocols do. Consequently, creation of a typical REST API requires additional effort to design and implement its own unique custom RPC protocol.

## Goals

REST-RPC is an attempt to demonstrate that the custom RPC protocol design and implementation effort is unnecessary by providing the simplest possible standard protocol which still supports features typically used by REST APIs. It is directly inspired by [JSON-RPC](https://www.jsonrpc.org/specification) and can be understood as its minimalistic HTTP-dependent sibling.

In other words, REST-RPC prescribes those REST-style API aspects which make no practical difference but preserves the flexibility to choose those aspects which do.

## Features

* HTTP as transport protocol
* Structured data messages in JSON format
* Binary data messages
* API function name in URL path
* API function arguments must have names
* API function arguments can be supplied either in the request body or in special cases as URL query parameters
* HTTP POST or GET method must be used to obtain standard or cached call semantics respectively

## Fairly anticipated questions (FAQ)

### When to use REST-RPC ?

In case any of the following remote API concerns need to be addressed with minimal effort:
* Caching GET requests
* Submitting or retrieving binary data
* External constraints requiring a JSON over HTTP REST-style API
 
In other situations it is [probably](https://youtu.be/XyJh3qKjSMk?t=53) better to use an established remote call protocol such as:
- [JSON-RPC](https://en.wikipedia.org/wiki/JSON-RPC)
- [Avro](https://en.wikipedia.org/wiki/Apache_Avro)
- [GraphQL](https://en.wikipedia.org/wiki/GraphQL)

### Why call it REST-RPC though it has little in common with REST concepts ?

To illustrate that it provides remote API authors with a solution of equivalent capability to typical REST API protocols by employing RPC principles.

### Can it be used in practice without a specific REST-RPC library ?

Absolutely. Any REST client or server library will suffice. However, using a specific REST-RPC library minimizes the implementation effort.

## Request

### HTTP method

HTTP methods are not specified by the API but chosen by the client from the following options depending on the desired call semantics:
* POST - standard non-cached call with arguments either in the request body or in special cases as URL query parameters
* GET - cacheable call with arguments as URL query parameters only

### URL format

* Remote API endpoint: http://example.org/api
* Remote API function: hello
* Remote API function arguments:
  * some = world
  * n = 1

```html
http://example.org/api/hello?some=world&n=1
```

* URL path components following an API-dependent prefix must specify the invoked function
* URL query parameters may specify additional arguments for the invoked function for GET requests or binary POST requests

Identically named invoked function arguments must not be supplied both in the request body and as URL query parameter. Such an ambiguous call must cause an error.

### Structured request body

All invoked function arguments must be supplied in the request body consisting of a JSON object with its field names representing the remote function parameter names and field values their respective argument values. Invoked function arguments must not be specified as URL query parameters

- Message format: JSON
- Method: POST
- Content-Type: application/json

**Remote call**

```scala
hello(some = "world", n = 1)
```

**Request headers**

```html
POST http://example.org/api/hello
Content-Type: application/json
```

**Request body**

```json
{
  "some": "world",
  "n": 1
}
```

### Empty request body

All invoked function arguments must be supplied as URL query parameters with query parameter names representing the remote function parameter names and query parameter values their respective argument values. Multiple instances of identically named query parameters must not be used.

- Method: GET

**Remote call**

```scala
hello(some = "world", n = 1)
```

**Request headers**

```html
GET http://example.org/api/hello?some=world&n=1
```

**Request body**

*Empty*

### Binary request body

Request body is interpreted as a first argument of the invoked function representing an array of bytes. Additional invoked function arguments may be supplied as URL query parameters with query parameter names representing the remote function parameter names and query parameter values their respective argument values. Multiple instances of identically named query parameters must not be used.

- Message format: binary
- Method: POST
- Content-Type: application/octet-stream

**Remote call**

```scala
hello(data = binary, some = "world", n = 1)
```

**Request headers**

```html
POST http://example.org/api/hello?some=test&n=1
Content-Type: application/octet-stream
```

**Request body**

*Binary data*

## Response

### Structured response body

Response body is interpreted as a successful invocation result if it consists of a JSON object containing a `result` field. The `result` field value represents the return value of the invoked remote function.

- Message format: JSON
- Content-Type: application/json

**Response headers**

```html
Content-Type: application/json
```

**Response body**

```json
{
  "result": "test"
}
```

### Binary response body

Response body is interpreted as a return value of successfully invoked remote function representing an array of bytes.

- Message format: binary
- Content-Type: application/octet-stream

**Response headers**

```html
Content-Type: application/octet-stream
```

**Response body**

*Binary data*

### Error response body

Response body is interpreted as a failed invocation result if it consists of a JSON object containing an `error` field. The `error` field value is a JSON object providing further information about the failure and consisting of the following fields:

* `message` - A JSON string representing an error message. This field is mandatory.
* `code` - A JSON number representing an error code. This field is optional.
* `details` - An arbitrary JSON value representing additional error information. This field is optional.

- Message format: JSON
- Content-Type: application/json

**Response headers**

```html
Content-Type: application/json
```

**Response body**

```json
{
  "error": {
    "message": "Some error",
    "code": 1,
    "details": {
      ...
    }
  }
}
```
