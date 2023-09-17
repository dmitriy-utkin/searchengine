# searchengine

### Education project

---

Hello there :wave:

Here you can find a search engine module, it is working with prepared root sites url list to make the full indexation for all the child links and create the index by page/word.

You can use the backend part like a search module for any website.

![searchengine_preview](src/main/resources/searchengineReadme/searchengine_preview.gif)
___

## How does it work?

To make a search by marked sites (you can add it to the config file to have an opportunity of searching by the pages of this site) was created a search index.

### What is the "search index"?
This is a data structure (entity) that stores information about the found words in the "normal forms" and page where it was found. Like on page 1 was found words "A", "B", "C" and on page 2 was found words "D", "E", "F" and "A". So we want to find word "A": application will check which pages contain this words and reply to us two page (1 and 2). 

### What is the "normal form"?
The "normal form" is a base form of each word. I used a lucene.morphology api ([to check details about this api click here](https://lucene.apache.org)).
```xml
<dependency>
    <groupId>org.apache.lucene.morphology</groupId>
    <artifactId>morph</artifactId>
    <version>${lucene.version}</version>
</dependency>
```

### How works a search by the requested words:
1. Firstly apps founds normal words via lucene api and filters it by frequency (if this lemma contains on the site more than percent from search-setting config it is not used for searching)
2. Then apps create the SearchQueryObject with:
    * lemma (as a key)
    * total frequency by lemma for the next sorting
    * list of lemmas entry from data base
    * list of indexes by these lemmas
    * and finally queryPages by these indexes (with page from data base and rank for this lemma+page from index)
3. Then apps filters of the pages by SearchQueryObject (with sorting by totalFrequency from low to high) for the presence of pages in the previous object
4. Finally, apps make a SearchQueryResult for the page with existed searched word and sort it by relative relevance. Relative relevance building by formula `"abs/maxAbs" - by list of QueryResuls`


## How launch a serchengine app


___
## Model:

![model_schema](src/main/resources/searchengineReadme/searchengine_db_schema.jpg)

### Site

Here we have an information about this site:
* Site ID
* Site url
* Site name
* Site status _(INDEXED / INDEXING / FAILED)_
* Site status time _(when it was updated last time?)_

### Page
Here we have information about each page by the sites:
* Page ID
* Page path _(uri)_
* Site ID for this page
* Page HTTP code
* Page HTML content

### Lemma
Here we have information about all the lemmas by sites:
* Lemma ID
* Site ID for this lemma
* Lemma
* Frequency by this lemma on site

### Index
Here we have information about indexes by lemma+page
* Index ID
* Page by this index
* Lemma by this index
* Lemma rank by this index (how many times this lemma was detected on page)

___
## Services:
### IndexingService
![indexing_service](src/main/resources/searchengineReadme/indexing.png)

This is the service for using an indexation functionality. Here you can find a main methods:
* startIndexing() - to launch full indexing of sites from config
* stopIndexing() - to stop an indexing
* indexPage(String url) - to launch an indexing for one page **(if this site is exists in config)**

### SearchService
![search_service](src/main/resources/searchengineReadme/search.png)

This is the service for using a search functionality. Here you can find only one main method:
* search(String query, String site, int offset, int limit) - to launch a locking for the search word by indexes

### StatisticsService
![statistics_service](src/main/resources/searchengineReadme/statistics.png)

This is the service to get actual information about application. Here you can find only one method as well:
* getStatistics() - to collect actual information: how many sites, pages, lemmas was indexing, also about each of the sites with detail info
### ResponseService

This is the last service, it is using to create a correct response for each of the previous methods(success or error response)

