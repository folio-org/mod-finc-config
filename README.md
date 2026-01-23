# mod-finc-config

Copyright (C) 2019-2026 The Open Library Foundation

This software is distributed under the terms of the Apache License, Version 2.0. See the
file "[LICENSE](LICENSE)" for more information.

![Development funded by European Regional Development Fund (EFRE)](assets/EFRE_2015_quer_RGB_klein.jpg)

## Introduction

Backend module for both `ui-finc-config` and `ui-finc-select`.

In general, the finc apps allow the maintenance of metadata sources and collection for the finc user
community.

This module works tenant-agnostic. That means, its data is stored in a single database schema. There
are no separate schemas for each tenant. This enables certain users (`ui-finc-config`) to have access
to data of all tenants that are members of the finc user community (`ui-finc-select`).

`ui-finc-config` gives access to the data as stored. Users of `ui-finc-config` can see which metadata
sources and collections are selected by which tenant/isil. In addition, it allows the configuration
of metadata sources and collections.

`ui-finc-select` provides a view on the same data as `ui-finc-config` but filtered for the current
tenant. It allows members of the finc user community (libraries) only to select or deselect metadata
collections as well as whole metadata sources.

For instance, a user of a specific tenant with an isil (International Standard Identifier for
Libraries and Related Organizations) may have access to the finc-select app. This user can only see
if its library has selected a certain metadata collection. In addition, the user can only
select/deselect a metadata collection for its library. Moreover, the user cannot see if other
libraries do have selected the metadata collection.

Only a user with granted permissions for `ui-finc-config` can edit the metadata source resp.
collection. In addition, only a user with these permissions is able to see which library selected
which metadata source resp. collection.

`mod-finc-config` does the translation between the *original* data used by `ui-finc-config` and the
*view* used by `ui-finc-select`. The *views* used by `ui-finc-select` are produced based on the tenant's
isil.

In order to implement the mentioned functionality, the module uses Okapi's `/_/tenant` interface in
a special way. If the module is registered for a new tenant, its own `/_/tenant` interface
overwrites the given `x-okapi-tenant` with the prefix predefined in the module, namely `finc`.

Hence, it is good practice to first create a Folio tenant named `finc`. Afterwards it is recommended
to activate the module for this tenant via the interface `_/proxy/tenants/finc/install`. This will
create the needed database schema.

In the next step the module can be activated for tenants as needed, e.g. tenant `diku`. Note, this
activation will not create any database schema.

When disabling the module for a tenant, the query parameter `purge=true` works only when disabling
the module for the tenant `finc`. This means it is impossible to purge the database schema for any
tenant different from `finc`.

## Quick Start (Standalone)

This section describes how to run the module standalone without Okapi. This is useful for
development and testing. For production deployments, use Okapi to manage the module.

### Prerequisites

- Java 21
- Maven
- Docker (for PostgreSQL)

### 1. Build

```bash
mvn clean install
```

### 2. Start PostgreSQL

Start a PostgreSQL container (same image used for integration tests):

```bash
docker run -d \
  --name postgres-finc \
  -e POSTGRES_USER=folio_admin \
  -e POSTGRES_PASSWORD=folio_admin \
  -e POSTGRES_DB=okapi_modules \
  -p 5432:5432 \
  postgres:16-alpine
```

### 3. Start the Module

```bash
DB_HOST=localhost DB_PORT=5432 DB_DATABASE=okapi_modules \
DB_USERNAME=folio_admin DB_PASSWORD=folio_admin \
java -jar target/mod-finc-config-fat.jar
```

The module starts on port 8081.

### 4. Initialize the Tenant

Initialize the `finc` tenant. The `x-okapi-url` header is required for tenant initialization
because the tenant API may load reference data or make callbacks. Regular API requests don't
need this header since they only interact with the local database.

Without sample data:

```bash
curl -X POST "http://localhost:8081/_/tenant" \
  -H "Content-Type: application/json" \
  -H "x-okapi-tenant: finc" \
  -H "x-okapi-url: http://localhost:8081" \
  -d '{"module_to": "mod-finc-config-7.0.0-SNAPSHOT"}'
```

With sample data (loads ISILs, metadata sources, and collections from `sample-data/`):

```bash
curl -X POST "http://localhost:8081/_/tenant" \
  -H "Content-Type: application/json" \
  -H "x-okapi-tenant: finc" \
  -H "x-okapi-url: http://localhost:8081" \
  -d '{
    "module_to": "mod-finc-config-7.0.0-SNAPSHOT",
    "parameters": [
      {
        "key": "loadSample",
        "value": "true"
      }
    ]
  }'
```

### 5. Example Requests

**Create an ISIL (tenant identifier):**

```bash
curl -X POST "http://localhost:8081/finc-config/isils" \
  -H "Content-Type: application/json" \
  -H "x-okapi-tenant: finc" \
  -d '{
    "id": "c0c14e3d-f731-4094-b40b-16e32b5e1a22",
    "library": "diku",
    "isil": "DIKU-01",
    "tenant": "diku"
  }'
```

**Create a metadata source:**

```bash
curl -X POST "http://localhost:8081/finc-config/metadata-sources" \
  -H "Content-Type: application/json" \
  -H "x-okapi-tenant: finc" \
  -d '{
    "id": "6dd325f8-b1d5-4568-a0d7-aecf6b8d6697",
    "label": "Cambridge University Press Journals",
    "description": "This is a test metadata source",
    "status": "active",
    "sourceId": 1
  }'
```

**Create a metadata collection:**

```bash
curl -X POST "http://localhost:8081/finc-config/metadata-collections" \
  -H "Content-Type: application/json" \
  -H "x-okapi-tenant: finc" \
  -d '{
    "id": "6dd325f8-b1d5-4568-a0d7-aecf6b8d6123",
    "label": "21st Century COE Program",
    "description": "This is a test metadata collection",
    "mdSource": {
      "id": "6dd325f8-b1d5-4568-a0d7-aecf6b8d6697",
      "name": "Cambridge University Press Journals"
    },
    "metadataAvailable": "yes",
    "usageRestricted": "no",
    "freeContent": "undetermined",
    "collectionId": "coe-123"
  }'
```

**List all metadata sources:**

```bash
curl "http://localhost:8081/finc-config/metadata-sources" \
  -H "x-okapi-tenant: finc"
```

**List all metadata collections:**

```bash
curl "http://localhost:8081/finc-config/metadata-collections" \
  -H "x-okapi-tenant: finc"
```

**Get tenant-filtered view (finc-select perspective):**

```bash
curl "http://localhost:8081/finc-select/metadata-sources" \
  -H "x-okapi-tenant: diku"
```

## Related Modules

### UI Modules

- [ui-finc-config](https://github.com/folio-org/ui-finc-config) - Admin interface for managing metadata sources and collections across all tenants
- [ui-finc-select](https://github.com/folio-org/ui-finc-select) - Library interface for selecting/deselecting metadata collections for a specific tenant

### Dependencies

This module has no required interface dependencies (no `requires` section in the ModuleDescriptor).

Optional dependencies declared in ModuleDescriptor:

- `organizations-storage.organizations` - Used to resolve organization names in metadata sources

Runtime behavior:

- **Organization names**: When returning metadata sources, the module calls
  `/organizations-storage/organizations/{id}` to resolve organization names. The lookup uses the
  calling user's permissions. If the lookup fails (interface unavailable or user lacks permission),
  the module logs a warning and continues without the name (see `OrganizationNameResolver.java`).
- **User contacts**: Stored as local references (UUIDs and names) without runtime lookups.

Related modules:

- [mod-organizations-storage](https://github.com/folio-org/mod-organizations-storage) - Optional runtime lookup for organization names
- [mod-users](https://github.com/folio-org/mod-users) - Source of user UUIDs stored in contacts (no runtime lookup)

## Filters

The following section describes how to upload, download and manage filters.

The management of filters is done by two endpoints: `/finc-select/filters` and `/finc-select/files`.

`/finc-select/filters` describes the filter. It has a `label` and a `type` which defines if this is
a `blacklist` or a `whitelist` filter. It also has `filterFiles` which is an array holding
information about associated files. The property `fileId` of `filterFiles` holds a reference (UUID)
to the file which was uploaded before (see `/finc-select/files`). `filename` is the local's filename
of the uploaded file.

`/finc-select/files` stores the actual binary file. A file is uploaded via HTTP POST to
`/finc-select/filter-files`, which returns the file's UUID. A single file can be downloaded via a
HTTP GET `/finc-select/filter-files/{id}`. Note, that you need to upload the binary file first, to
get its id, which can then be used in the definition of a filter document.

**File Upload Size Limit**: File uploads are limited to a maximum size of 50 MB to prevent denial
of service (DoS) attacks. If a file exceeds this limit, the upload will fail with a `413` (Payload
Too Large) error response. This limit applies to both `/finc-select/files` and `/finc-config/files`
endpoints.

**Gateway Configuration Required**: To support 50 MB file uploads, the API gateway and sidecar must be
configured with appropriate request size limits. Without these configurations, uploads will be rejected
at the gateway level before reaching this module.

For **Kong** (FOLIO gateway), add to the Kong container environment:

```yaml
KONG_NGINX_HTTP_CLIENT_MAX_BODY_SIZE: 100m
```

For **Eureka/Module Sidecar**, add to the sidecar environment configuration:

```yaml
QUARKUS_HTTP_LIMITS_MAX_BODY_SIZE: 100M
```

## Harvest EZB Holding Files

This module can harvest holding files from the *Elektronische Zeitschriftenbibliothek (EZB)*
periodically.

The harvester will run each night at 1am automatically. In order to activate automatic harvesting
for a certain tenant/library you need to define a filter called `EZB holdings` in finc-select. This
filter needs to have a file called `EZB file`. In addition, EZB credentials need to be defined for
the same tenant.

With this, the harvesting works as follows: The harvester fetches the EZB credentials of each tenant
and will harvest the holding file the tenants from the EZB. It will compare the downloaded holding
file with the file called `EZB file` of the filter `EZB holdings`. If both files differ, the `EZB
file` will be updated by the downloaded one. If the files are equal, nothing will be done.

Thus, to activate harvesting of holding files you need to define EZB credentials and add a filter
called `EZB holdings` with a file called `EZB file`.

The EZB URL needs to be configured using the environment variable `EZB_DOWNLOAD_URL`. This URL must include a `%s`
placeholder, which will be substituted with the `bidId` when processing requests.

Proxy settings are honored. Use system properties `http.proxyHost`, `http.proxyPort`,
`https.proxyHost`, `https.proxyPort`, `http.nonProxyHosts` or environment variables `HTTP_PROXY`,
`HTTPS_PROXY`, `NO_PROXY` if running as Docker container.
