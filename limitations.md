# Limitations
This library implements as many available features of both Liquibase and Cloud Spanner as possible. However, there
are a number of features that either can't be supported, or that can only be supported through custom SQL changes.
These limitations and possible workarounds are listed in this document.

## Cloud Spanner features without a corresponding Liquibase change type
The following Cloud Spanner features do not have a corresponding change type in Liquibase and are, therefore,
only supported through custom SQL change sets or through automatic modification of the SQL statements that are
generated by Liquibase.

Add a [ModifySql](https://docs.liquibase.com/workflows/liquibase-community/modify-sql.html) command to your change set
to modify the generated SQL. See the [create-schema.yaml](example/create-schema.yaml) file for some examples.

- Interleaved tables: Use `ModifySql` to append `, INTERLEAVE IN PARENT <parent>` to the `createTable` statement. See [create-schema.yaml](example/create-schema.yaml) for an example.
- Interleaved indexes: Use `ModifySql` to append `, INTERLEAVE IN PARENT <parent>` to the `createIndex` statement.
- Commit timestamp columns: Use `ModifySql` to replace the column definition with one that includes the `OPTIONS (allow_commit_timestamp=true)` clause. See [create-schema.yaml](example/create-schema.yaml) for an example.
- Null-filtered indexes: Use `ModifySql` to replace the `CREATE INDEX` statement with `CREATE NULL_FILTERED INDEX`. See [this test file](src/test/resources/create-null-filtered-index-singers-first-name.spanner.yaml) for an example.

## Database features not supported by Cloud Spanner
The following database features are not supported by Cloud Spanner, and trying to create/alter/drop any of them through Liquibase will cause an error.

- Auto increment columns
- Sequences
- Default value definition for a column
- Unique constraints: Use `UNIQUE INDEX` instead of `UNIQUE CONSTRAINT`
- Stored procedures
- Table and column remarks

## Liquibase change types with no Cloud Spanner support
The following change types are not supported by Cloud Spanner.
- Add/Drop primary key: Cloud Spanner requires that all tables have a primary key. The primary key must be defined when the table is created, and cannot be dropped or added later.
- Rename table: Cloud Spanner does not support renaming tables. Create a copy instead, and drop the old table.
- Rename column: Cloud Spanner does not support renaming columns. Create a copy instead, and drop the old column.

## Liquibase change types with limited Cloud Spanner support
The following Liquibase change types are implemented for Cloud Spanner but have certain limitations.

- AddLookupTable: This feature is implemented and works for most cases. However, if the amount of data to be inserted in the new lookup table exceeds any of the [Cloud Spanner transaction limits](https://cloud.google.com/spanner/docs/dml-tasks#transaction_limits), the change will fail and you should implement it using a custom SQL change.
- Delete: This feature is implemented and works for most cases. However, if the amount of data to be deleted in the new lookup table exceeds any of the [Cloud Spanner transaction limits](https://cloud.google.com/spanner/docs/dml-tasks#transaction_limits), the change will fail and you should implement it using a custom SQL change. Specifying a `WHERE` clause for the `DELETE` statement is required (the clause may be `WHERE TRUE`).
- Load data / Load-update data: These features are implemented and works for most cases. If however the amount of data to be inserted or updated exceeds any of the [Cloud Spanner transaction limits](https://cloud.google.com/spanner/docs/dml-tasks#transaction_limits), the change will fail and you should implement it using a custom SQL change.
- Modify data type: This feature works, but only for the data type changes that are [allowed by Cloud Spanner](https://cloud.google.com/spanner/docs/data-definition-language#description_3).

A potential work-around for the transaction limits in Cloud Spanner is to use [Partitioned DML](https://cloud.google.com/spanner/docs/dml-tasks#partitioned-dml) instead of transactional DML. Partitioned DML statements are not bound by the transaction limits, but are also not atomic. The Cloud Spanner JDBC driver that is used by Liquibase supports Partitioned DML by setting the `AUTOCOMMIT_DML_MODE` connection property to `PARTITIONED_NON_ATOMIC`:

```sql
SET AUTOCOMMIT = TRUE;
SET AUTOCOMMIT_DML_MODE = 'PARTITIONED_NON_ATOMIC';

-- The following statement is executed as Partitioned DML and is automatically committed.
UPDATE <table> SET <column>=<value> WHERE TRUE;

SET AUTOCOMMIT_DML_MODE = 'TRANSACTIONAL';
```

## DDL limits

Cloud Spanner recommends some [best practices for schema updates](https://cloud.google.com/spanner/docs/schema-updates#best-practices) including limiting the frequency of schema updates and to consider the impact of large scale schema changes. One approach it to apply a small number of change sets. Alternatively, use [SQL change](https://docs.liquibase.com/change-types/community/sql.html) and batch the DDL using [batch statements](https://cloud.google.com/spanner/docs/use-oss-jdbc#batch_statements).
