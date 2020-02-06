## 1.4.0
* Finc-select metadata sources can be filtered by select status of collections (UIFC-42)
* Bugfix: Return correct "totalRecords"
* Add several database indexes
* Remove duplicate maven-failsafe-plugin from pom.xml (UIFC-117)

## 1.3.0
* Replace uniqueIndex for labels (UIFC-104)
* Update to RMB 29 (UIFC-100)
* Bugfix: Sortby does not work in filters (UIFC-99)
* Bugfix: Sorting in finc select throws error (UIFC-97)
* Bugfix: No response if isil is not found when fetching finc-select metadata-source (UIFC-69)
* Consider usageRestricted for finc-select metadata collection (UIFC-71)
* Bugfix: Combination of selected and permitted filter in finc-select (UIFC-81)

## 1.2.0
* Add endpoint to add/remove filters from finc-select metadata collections (UIFC-84)
* Add test suite to manage and structure tests
* ID is optional in filters (UIFC-66)
* Rename and add permissions (UIFC-16)
* Delete filter file if filter is deleted and vice versa (UIFC-62)

## 1.1.0
* Add finc-select/filter-files and finc-select/files to MD.json (UIFC-61)
* Allow multiple values for selected and permitted filter (finc select) (UIFC-57)

## 1.0.0
* Backend supports filter for metadata source in collections list view (UIFC-46)
* QueryTranslator throws exception if query is null (UIFC-50)
* Handles queries for finc-select metadata collections (UIFC-41)
* Switch from mod-vendors to mod-organizations-storage (UIFC-25) 
* Select all metadata sources in finc-selct (UIFC-26)
* Bugfix: Put metada source in finc-config (UIFC-27)
* More meaningful error messages (UIFC-30)

## 0.1.0
* UIFC-8 CRUD for metadata sources in finc-config
* UIFC-9 CRUD for metadata collections in finc-config
