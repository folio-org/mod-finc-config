# mod-finc-config

Copyright (C) 2019-2020 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Introduction

Backend module for finc-config and finc-select.

The finc apps allow the maintenance of metadata sources and collection for the finc user community. 

finc-config allows the configuration of metadata sources and collections. finc-select allows members of the finc user community (libraries) to select or deselect metadata collections as well as whole metadata sources.

This module works tenant-agnostic. That means, its data is not stored separated by tenant. finc-config gives access to the data as stored. finc-select provides a view on the data filtered for the current tenant. For instance, a user of a specific tenant with an isil (International Standard Identifier for Libraries and Related Organizations) may have access to the finc-select app. This user can only see if its library has selected a certain metadata collection. In addition, the user can only select/deselect a metadata collection for its library. Moreover, the user cannot see if other libraries do have selected the metadata collection.

Only a user with granted permissions for finc-select can edit the metadata source resp. collection. In addition, only a user with this permissions is able to see which library selected which metadata source resp. collection.

In order to implement the mentioned functionality, the module uses Okapi's */_/tenant* interface in a special way. If the module is registered for a new tenant, its own */_/tenant* interface overwrites the given *x-okapi-header* with the prefix predefined in the module, namely *finc*.

## Filters

The following section describes how to upload, download and manage filters.

The management of filters is done by two endpoints: */finc-select/filters* and */finc-select/files*.

*/finc-select/filters* describes the filter. It has a *label* and a *type* which defines if this is a *blacklist* or a *whitelist* filter. It also has *filterFiles* which is an array holding information about associated files. The property *fileId* of *filerFiles* holds a reference (uuid) to the file which was uploaded before (see */finc-select/files*). *Filename* is the local's filename of the uploaded file.

*/finc-select/files* stores the actual binary file. A file is uploaded via HTTP POST to */finc-select/filter-files*, which returns the file's uuid. A single file can be downloaded via a HTTP GET */finc-select/filter-files/{id}*. Note, that you need to upload the binary file first, to get its id, which can then be used in the definition of a filter document.
