# Antra SEP java evaluation project

## What did I do:
### 0. set up environment as well as email sending functionality
### 1. Add new features like update/delete/edit report.
		1. added delete API:
			for ClientServer, implemented deleteFile API and wrote aop handler handle deleteFile method's exception
			for ExcelServer,  implemented deleteExcel API and defined FileDeletionException then wrote ExceptionHandler to handle it.
			for PDFServer， wrote function to delete file from S3
		2. added PaginatedGet API 
		3. made API documentation for ClientService by using swagger.
			
### 2. Improve sync API performance by using multithreading and sending request concurrently to both services.
		used Completable 

### 3. Use a database instead of hashmap in the ExcelRepositoryImpl. 
		1. config AWS RDS MySql database 
		2. Implemented old dao layer APIs for APIs consistency
		3. defined excelFileEntity then wrote ExcelFileConverter convert enity to excelfile

### 4. Improve code coverage by adding more tests.
		1. add delete API test used rest-assured and mockito
		
### 5. Convert sync API into microservices by adding Eureka/Ribbon support.

### 6. Fix bugs.
		1. ExcelService/service/ExcelGenerationServicelmpl line 110 must close outputStream in order to delete file.
		2. downGrade rest-assured to 3.3.0 to make ExcelService test case works.(4.3.0 not work)
### 7. Make the system more robust by adding fault tolerance such like : DeadLetter Queue, retry, cache, fallback etc.
		Used Hystrix set up fallback method for ClientService.ReportController (need test)



