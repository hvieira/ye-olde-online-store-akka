# ye-olde-online-store-akka

An exercise of what an online store built with scala and akka might look like. 

## Tech
- web server - AKKA HTTP
- AKKA for actors that define concrete processes which need concurrency and proper failure handling
- authentication - JWT tokens for authenticated requests

## Build and test
`activator clean test package`

## Run
`activator run`

### Making requests
Get a authenticated token 

`curl -v --data "username=john&password=mary"  "localhost:9000/login"`

A auth token is returned which can then be used in subsequent requests. For example to retrieve the user cart

`curl -v -X GET -H "Authorization: Bearer <token>" "localhost:9000/user/cart"`

Put items in the cart

`curl -v -X PUT -H "Authorization: Bearer <token>" -H "Content-Type: application/json" --data '{"itemId":"sanity potion", "amount": 1}' "localhost:9000/user/cart"`

Checkout

`curl -v -X POST -H "Authorization: Bearer <token>" "localhost:9000/purchase"`




