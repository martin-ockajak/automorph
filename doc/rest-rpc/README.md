# REST-RPC

REST-RPC is an RPC protocol prescribing a standard way to model REST APIs while taking reusing existing tools in the REST ecosystem. 

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
    "details": {
    }
  }
}
```
