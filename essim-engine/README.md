#ESSIM engine

## OpenAPI
There is an openapi UI available at `/openapi/`



## Config
### Environment

```

PRINT_TREE	If this env variable has been set, it will generate the D3 Tree HTML for each network in the ESDL

```

### InfluxDB credentials
if you put a `credentials.json` in this folder, ESSIM will use it to access remote InfluxDB servers that require authentication.
Format is like this:

```json
{
  "http://server:port": {
	"username": "username",
	"password": "password"
  }
}

```

