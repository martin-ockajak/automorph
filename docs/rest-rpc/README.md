# REST-RPC

REST-RPC is an RPC protocol prescribing a standard way to model REST APIs while reusing existing tools in the REST ecosystem.

## Motivation

REST as a theoretical concept is deemed independent of specific data formats or transport protocols. However, in practice RESTful APIs are typically built for web applications by transforming remote function calls into JSON messages over HTTP. Such transformations require deciding where in the underlying protocol to store various RESTful call data and metadata.

Consequently, **every RESTful API** design requires also designing its own **custom** remote call (RPC) **protocol**.

REST-RPC is an attempt to make the **custom** protocol design step **unnecessary** by **providing a standard** protocol while retainig REST APIs suitability for **web applications**.

## Request

```json
{
  "argument1": "test",
  "argument2": 0,
  "argument3": true
}
```

## Result response

```json
{
  "result": "test"
}
```

## Error response

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
