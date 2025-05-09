CREATE OR REPLACE FUNCTION execute_query_on_multiple_tables(
    schema_name TEXT,  -- Dynamically retrieve tables from this schema
    the_query TEXT     -- Query to execute for each table, using {TABLE_NAME} as placeholder
)
RETURNS TABLE(
    table_name TEXT,
    group_by_label TEXT,
    result_value NUMERIC
) LANGUAGE plpgsql AS $$
DECLARE
    fetched_table_name TEXT;
    dynamic_query TEXT;
BEGIN
    -- Loop through all the table names dynamically retrieved from the given schema
    FOR fetched_table_name IN
        SELECT t.table_name
        FROM information_schema.tables t
        WHERE t.table_schema = schema_name
    LOOP
        -- Replace the placeholder '{TABLE_NAME}' in the query with the actual table name
        dynamic_query := replace(
                            the_query,
                            '{TABLE_NAME}',
                            format('%I.%I', schema_name, fetched_table_name)
                         );

        -- Execute the dynamic query and return its results, along with the table name
        RETURN QUERY EXECUTE format(
            'SELECT %L AS table_name, t.group_by_label, t.result_value::NUMERIC AS result_value FROM (%s) t',
            fetched_table_name,
            dynamic_query
        );
    END LOOP;
END $$;