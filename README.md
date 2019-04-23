# mod-finc-config

## Introduction

Knowledge base for amsl's finc-config module.

This module works tenant-agnostic. That means, its data is not stored separated by tenant.

The module uses Okapi's */_/tenant* interface in a special way. If the module is registered for a new tenant, its own */_/tenant* interface overwrites the given *x-okapi-header* with the prefix predefined in the module, namely *finc*.
On the other hand, this means, that only the tenant *finc* can be deleted.
