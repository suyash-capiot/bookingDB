package com.coxandkings.travel.bookingengine.db.postgres.common;


import org.hibernate.type.StandardBasicTypes;
import org.hibernate.dialect.PostgreSQL94Dialect;

import java.sql.Types;

//extending the PostgreSQL dialect to tell it about the json type
public class JsonPostgreSQLDialect extends PostgreSQL94Dialect  {

    public JsonPostgreSQLDialect() {

        super();

        this.registerColumnType(Types.JAVA_OBJECT, "jsonb");
        registerHibernateType( Types.OTHER, StandardBasicTypes.CLASS.getName() );
    }
}