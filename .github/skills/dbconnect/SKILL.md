---
name: dbconnect
description: Guide for using no.nav.aap.komponenter.dbconnect in Kotlin. Use when adding or refactoring database access, transactions, queries, parameters, row mapping, or batch writes.
---

Use this skill when working with the `no.nav.aap.komponenter.dbconnect` package.

Repository: https://github.com/navikt/aap-kelvin-komponenter

## Core flow

1. Start database work with `DataSource.transaction { connection -> ... }`.
2. Use `readOnly = true` for read-only work.
3. Keep commit and rollback handling inside the transaction helper.
4. Use `markerSavepoint()` only when you need a partial rollback point inside a larger transaction.

## Writing data

- `connection.execute(sql)` for INSERT, UPDATE, DELETE, and DDL.
- `connection.executeReturnUpdated(sql)` when you need the affected row count.
- `connection.executeReturnKey(sql)` and `connection.executeReturnKeys(sql)` for generated keys.
- `connection.executeBatch(sql, elements) { setParams { element -> ... } }` for bulk writes.
- Call `setParams` only once per `Execute`, `Query`, or `ExecuteBatch` block.

## Querying data

- Use `queryFirst`, `queryFirstOrNull`, `queryList`, or `querySet`.
- Always call `setRowMapper { row -> ... }`.
- Map each row to a local domain type instead of returning `Row` from repositories.
- Use `setQueryTimeout(sekunder)` only when you need a different timeout; valid values are 1 to 300.

## Parameters

Use the matching `Params` helper for the JDBC type:

- `setString`, `setBytes`, `setEnumName`, `setInt`, `setLong`, `setDouble`
- `setBigDecimal`, `setUUID`, `setBoolean`
- `setLocalDate`, `setLocalDateTime`, `setInstant`
- `setPeriode`, `setProperties`
- `setArray`, `setLongArray`, `setUUIDArray`, `setPeriodeArray`

Prefer nullable setters for optional values.

## Reading values

Use `Row` helpers that match the column type:

- `getString` / `getStringOrNull`
- `getInt` / `getIntOrNull`
- `getLong` / `getLongOrNull`
- `getBoolean` / `getBooleanOrNull`
- `getLocalDate`, `getLocalDateTime`, `getInstant`
- `getEnum`, `getEnumOrNull`
- `getUUID`, `getUUIDOrNull`
- `getPeriode`, `getPeriodeOrNull`
- `getArray(columnLabel, KClass)` when reading JDBC arrays

Prefer `...OrNull` helpers when the column can be null.

## Transaction behavior

- `transaction` commits on success and rolls back on failure.
- `readOnly = true` still rolls back on exit to release locks.
- Do not swallow database exceptions unless the caller needs to translate them.

## Good patterns

- Batch inserts should use the built-in `executeBatch` helper.
- Read-only repository methods should stay inside one transaction and use `queryList` or `queryFirstOrNull`.
