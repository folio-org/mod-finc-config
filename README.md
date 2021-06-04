# mod-finc-config

Copyright (C) 2019-2021 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

## Introduction

Backend module for both ui-finc-config and ui-finc-select.

In general, the finc apps allow the maintenance of metadata sources and collection for the finc user community. 

This module works tenant-agnostic. That means, its data is stored in a single database schema. There are no separate schemas for each tenant. This enables certain users (ui-finc-config) to have access to data of all tenants that are members of the finc user community (ui-finc-select). 

ui-finc-config gives access to the data as stored. Users of ui-finc-config can see which metadata sources and collections are selected by which tenant/isil. In addition, it allows the configuration of metadata sources and collections.

ui-finc-select provides a view on the same data as ui-finc-config but filtered for the current tenant. It allows members of the finc user community (libraries) only to select or deselect metadata collections as well as whole metadata sources.

For instance, a user of a specific tenant with an isil (International Standard Identifier for Libraries and Related Organizations) may have access to the finc-select app. This user can only see if its library has selected a certain metadata collection. In addition, the user can only select/deselect a metadata collection for its library. Moreover, the user cannot see if other libraries do have selected the metadata collection.

Only a user with granted permissions for ui-finc-config can edit the metadata source resp. collection. In addition, only a user with these permissions is able to see which library selected which metadata source resp. collection.

mod-finc-config does the translation between the _original_ data used by ui-finc-config und the _view_ used by ui-finc-select. The _views_ used by ui-finc-select are produced based on the tenant's isil.

In order to implement the mentioned functionality, the module uses Okapi's */_/tenant* interface in a special way. If the module is registered for a new tenant, its own */_/tenant* interface overwrites the given *x-okapi-header* with the prefix predefined in the module, namely *finc*.

Hence, it is good practice to first create a Folio tenant named _finc_. Afterwards it is recommended to activate the module for this tenant via the interface `_/proxy/tenants/finc/install`. This will create the needed database schema.

In the next step the module can be activated for tenants as needed, e.g. tenant _diku_. Note, this activation will not create any database schema. 

When disabling the module for a tenant, the query parameter `purge=true` works only when disabling the module for the tenant _finc_. This means it is impossible to purge the database schema for any tenant different as _finc_.


## Filters

The following section describes how to upload, download and manage filters.

The management of filters is done by two endpoints: */finc-select/filters* and */finc-select/files*.

*/finc-select/filters* describes the filter. It has a *label* and a *type* which defines if this is a *blacklist* or a *whitelist* filter. It also has *filterFiles* which is an array holding information about associated files. The property *fileId* of *filerFiles* holds a reference (uuid) to the file which was uploaded before (see */finc-select/files*). *Filename* is the local's filename of the uploaded file.

*/finc-select/files* stores the actual binary file. A file is uploaded via HTTP POST to */finc-select/filter-files*, which returns the file's uuid. A single file can be downloaded via a HTTP GET */finc-select/filter-files/{id}*. Note, that you need to upload the binary file first, to get its id, which can then be used in the definition of a filter document.

## Harvest EZB holding files

This module can harvest holding files from the *Elektronische Zeitschriftenbibliothek (EZB)* periodically.

The harvester will run each night at 1am automatically. 
In order to activate automatic harvesting for a certain tenant/library you need to define a filter called *EZB holdings* in finc-select. This filter needs to have a file called *EZB file*. In addition, EZB credentials need to be defined for the same tenant.

With this, the harvesting works as follows: The harvester fetches the EZB credentials of each tenant and will harvest the holding file the tenants from the EZB. It will compare the downloaded holding file with the file called *EZB file* of the filter *EZB holdings*. If both files differ, the *EZB file* will be updated by the downloaded one. If the files are equal, nothing will be done.

Thus, to activate harvesting of holding files you need to define ezb credentials and add a filter called *EZB holdings* with a file called *EZB file*.

