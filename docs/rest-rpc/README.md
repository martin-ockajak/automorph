# REST-RPC

REST-RPC is an RPC protocol prescribing a standard way to model REST APIs while reusing existing practices and tools in the REST ecosystem.

## Motivation

[REST](https://en.wikipedia.org/wiki/Representational_state_transfer) as a theoretical concept is deemed independent of specific data formats, transport protocols or even calling conventions. However, in practice the vast majority REST APIs are created for web applications and services by transforming HTTP requests and responses in JSON format to remote API function calls.

Such transformations require deciding where to store various REST API call data and metadata in the underlying transport protocol and message format in the same way [RPC](https://en.wikipedia.org/wiki/Remote_procedure_call) protocols do. Consequently, creation of a typical REST API requires additional effort to design and implement its own unique custom RPC protocol.

## Goals

REST-RPC is an attempt to demonstrate that the custom RPC protocol design and implementation effort is unnecessary by providing the simplest possible standard protocol which still supports features typically used by REST APIs. In other words, REST-RPC prescribes those REST API aspects which make no practical difference but provides enough flexibility for those aspects which do.

## Features

* HTTP as transport protocol
* Structured data messages in JSON format
* Binary data messages
* API function name in URL path
* API function arguments must have names
* API function arguments can be supplied either in the request body or as URL query parameters
* HTTP POST or GET method must be used to obtain standard or cached call semantics respectively

## Fairly anticipated questions (FAQ)

### When to use REST-RPC ?

When external constraints require use of REST-style API but minimal amount of effort is desired. In other situations it is [probably](https://youtu.be/XyJh3qKjSMk?t=53) better to use an established remote call protocol such as:
- [JSON-RPC](https://en.wikipedia.org/wiki/JSON-RPC)
- [Avro](https://en.wikipedia.org/wiki/Apache_Avro)
- [GraphQL](https://en.wikipedia.org/wiki/GraphQL)

### Why call it REST-RPC though it has little in common with REST concepts ?

To illustrate the fact that it provides remote API authors with a solution equivalent to typical REST API protocols by employing RPC principles.

### Can it be used in practice without having a specific REST-RPC library ?

Absolutely. Any REST client or server library will suffice. However, using a specific REST-RPC library reduces the implementation effort.

## Request

### Method

HTTP methods are not specified by the API but chosen by the client from the following options depending on the desired call semantics:
* POST - standard non-cached call with arguments either in the request body or as URL query parameters
* GET - cacheable call with arguments as URL query parameters only

### URL

```html
http://authority/API/FUNCTION?PARAMETER1=argument1&PARAMETER2=argument2 ...
```

* URL path components following an API-dependent prefix must specify the invoked function
* URL query parameters may specify additional string arguments for the invoked function

Identically named invoked function arguments must not be supplied both in the request body and as URL query parameter. Such an ambiguous call must cause an error.

### Structured request body

Message in JSON format.

- Method: POST
- Content-Type: application/json

```json
{
  "argument1": "test",
  "argument2": 0,
  "argument3": true
}
```

### Empty request body

All invoked function arguments must be specified as URL query parameters.

- Method: GET

### Binary request body

Message in binary format. Request body is interpreted as a first argument of the invoked function.

- Method: POST
- Content-Type: application/octet-stream

## Response

### Structured result response body

Message in JSON format.

- Content-Type: application/json

```json
{
  "result": "test"
}
```

### Binary result response body

Message in binary format. Response body is interpreted as a successful result of the invoked function.

- Method: POST
- Content-Type: application/octet-stream

### Error response body

Message in JSON format.

- Content-Type: application/json

```json
{
  "error": {
    "code": 0,
    "message": "Some error",
    "data": {
    }
  }
}
```
