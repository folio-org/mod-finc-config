# 5.3.0
* [UIFC-275](https://issues.folio.org/browse/UIFC-275) RMB v34 upgrade

# 5.2.0
* [UIFC-263](https://issues.folio.org/browse/UIFC-263) Update to the latest RMB 33.* release
* [UIFC-265](https://issues.folio.org/browse/UIFC-265) EZBHarvestJob is using wrong URL
* [UIFC-266](https://issues.folio.org/browse/UIFC-266) Honor proxy settings when downloading EZB files

# 5.1.2
[UIFC-261](https://issues.folio.org/browse/UIFC-261) RMB 33.2.2 fixing remote execution (CVE-2021-44228)
[UIFC-260](https://issues.folio.org/browse/UIFC-260) RMB 33.2.1 fixing remote execution (CVE-2021-44228)

# 5.1.1
* Bugfix: Add exec-maven-plugin again so that project can be built (UIFC-251)

# 5.1.0
* Upgrade to RMB 33 (UIFC-241)
* Make backend perm not visible

# 5.0.0
* Upgrade to RMB 32 (UIFC-227)

# 4.0.1
* Upgrade to RMB 31.1.5 and Vert.x 3.9.4 (UIFC-223)

# 4.0.0
* Upgrade to RAML Module Builder 31.x
* Upgrade module to JDK 11 (UIFC-211)

# 3.0.0
* Restructure contacts of metadata-sources (UIFC-181)

# 2.1.2
* Bugfix: Migrate unknown publication value to undetermined

# 2.1.1
* Bugfix migration script

# 2.1.0
* Add POST and DELETE to finc-config/files (UIFC-203)
* Bugfix: FilterToCollections not removed when deleting filter (UIFC-204)
* Bugfix: Wrong filters displayed in finc-select (UIFC-207)
* Bugfix: Tenants should not be allowed multiple ISILs (UIFC-205)
* Rename Status "Wish" to "Request" (UIFC-214)

# 2.0.1
* Bugfix: Add missing finc-config/filters endpoints to ModuleDescriptor

# 2.0.0
* Add endpoints to manage association of filters and collections (UIFC-147, UIFC-149)
* Bugfix: finc-select > collections: Usage permitted filter does not work properly (UIFC-159)
* Add record las updated info to entities (UIFC-155)
* Add filters and files endpoint to finc-config (UIFC-169)
* Upgrade to RMB v30 (UIFC-168)
* Automatically harvest EZB files as filter files for tenants (UIFC-176)
* Delete field "Contracts" from metadata-sources (UIFC-185)
* Update metadata sources implementation status values/filter (UIFC-183)
* Collection id of metadata-collections is mandatory (UIFC-188)
* Enable field search on description for sources & collections (UIFC-172)
* Alphabetical sorting tiny metadata-sources (UIFC-178)
* Improve estimation of number of records (UIFC-180)
* Remove field "facet label" from metadata-collections (UIFC-196)
* Bugfix: Selected filter does not work as expected (UIFC-190)
* Field "collection id" of metadata-collections is unique (UIFC-192)

## 1.5.0
* Filters contain IDs of associated metadata collections (UIFC-136)
* Resolve and keep updated name of associated metadata source (UIFC-126)
* Bugfix: Finc-Select metada collection selected=yes not working (UIFC-129)

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
