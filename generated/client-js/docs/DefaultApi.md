# FlightbookClient.DefaultApi

All URIs are relative to *http://localhost:8080/api*

Method | HTTP request | Description
------------- | ------------- | -------------
[**createReservation**](DefaultApi.md#createReservation) | **POST** /reservations | Réserver un vol
[**getCompagnies**](DefaultApi.md#getCompagnies) | **GET** /compagnies | Liste des compagnies aériennes
[**getVolById**](DefaultApi.md#getVolById) | **GET** /vols/{id} | Détail d&#39;un vol
[**getVols**](DefaultApi.md#getVols) | **GET** /vols | Liste tous les vols disponibles
[**getVolsByCompagnie**](DefaultApi.md#getVolsByCompagnie) | **GET** /compagnies/{nom}/vols | Vols d&#39;une compagnie spécifique



## createReservation

> Reservation createReservation(volId)

Réserver un vol

### Example

```javascript
import FlightbookClient from 'flightbook-client';
let defaultClient = FlightbookClient.ApiClient.instance;
// Configure Bearer (JWT) access token for authorization: bearerAuth
let bearerAuth = defaultClient.authentications['bearerAuth'];
bearerAuth.accessToken = "YOUR ACCESS TOKEN"

let apiInstance = new FlightbookClient.DefaultApi();
let volId = 1; // Number | 
apiInstance.createReservation(volId, (error, data, response) => {
  if (error) {
    console.error(error);
  } else {
    console.log('API called successfully. Returned data: ' + data);
  }
});
```

### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **volId** | **Number**|  | 

### Return type

[**Reservation**](Reservation.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## getCompagnies

> [String] getCompagnies()

Liste des compagnies aériennes

### Example

```javascript
import FlightbookClient from 'flightbook-client';
let defaultClient = FlightbookClient.ApiClient.instance;
// Configure Bearer (JWT) access token for authorization: bearerAuth
let bearerAuth = defaultClient.authentications['bearerAuth'];
bearerAuth.accessToken = "YOUR ACCESS TOKEN"

let apiInstance = new FlightbookClient.DefaultApi();
apiInstance.getCompagnies((error, data, response) => {
  if (error) {
    console.error(error);
  } else {
    console.log('API called successfully. Returned data: ' + data);
  }
});
```

### Parameters

This endpoint does not need any parameter.

### Return type

**[String]**

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## getVolById

> Vol getVolById(id)

Détail d&#39;un vol

### Example

```javascript
import FlightbookClient from 'flightbook-client';
let defaultClient = FlightbookClient.ApiClient.instance;
// Configure Bearer (JWT) access token for authorization: bearerAuth
let bearerAuth = defaultClient.authentications['bearerAuth'];
bearerAuth.accessToken = "YOUR ACCESS TOKEN"

let apiInstance = new FlightbookClient.DefaultApi();
let id = 56; // Number | 
apiInstance.getVolById(id, (error, data, response) => {
  if (error) {
    console.error(error);
  } else {
    console.log('API called successfully. Returned data: ' + data);
  }
});
```

### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **id** | **Number**|  | 

### Return type

[**Vol**](Vol.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## getVols

> [Vol] getVols()

Liste tous les vols disponibles

### Example

```javascript
import FlightbookClient from 'flightbook-client';
let defaultClient = FlightbookClient.ApiClient.instance;
// Configure Bearer (JWT) access token for authorization: bearerAuth
let bearerAuth = defaultClient.authentications['bearerAuth'];
bearerAuth.accessToken = "YOUR ACCESS TOKEN"

let apiInstance = new FlightbookClient.DefaultApi();
apiInstance.getVols((error, data, response) => {
  if (error) {
    console.error(error);
  } else {
    console.log('API called successfully. Returned data: ' + data);
  }
});
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**[Vol]**](Vol.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json


## getVolsByCompagnie

> [Vol] getVolsByCompagnie(nom)

Vols d&#39;une compagnie spécifique

### Example

```javascript
import FlightbookClient from 'flightbook-client';
let defaultClient = FlightbookClient.ApiClient.instance;
// Configure Bearer (JWT) access token for authorization: bearerAuth
let bearerAuth = defaultClient.authentications['bearerAuth'];
bearerAuth.accessToken = "YOUR ACCESS TOKEN"

let apiInstance = new FlightbookClient.DefaultApi();
let nom = "Air France"; // String | 
apiInstance.getVolsByCompagnie(nom, (error, data, response) => {
  if (error) {
    console.error(error);
  } else {
    console.log('API called successfully. Returned data: ' + data);
  }
});
```

### Parameters


Name | Type | Description  | Notes
------------- | ------------- | ------------- | -------------
 **nom** | **String**|  | 

### Return type

[**[Vol]**](Vol.md)

### Authorization

[bearerAuth](../README.md#bearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

