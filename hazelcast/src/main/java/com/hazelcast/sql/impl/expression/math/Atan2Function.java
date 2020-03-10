/*
 * Copyright (c) 2008-2020, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.sql.impl.expression.math;

import com.hazelcast.sql.HazelcastSqlException;
import com.hazelcast.sql.impl.expression.BiCallExpression;
import com.hazelcast.sql.impl.expression.Expression;
import com.hazelcast.sql.impl.row.Row;
import com.hazelcast.sql.impl.type.QueryDataType;

/**
 * ATAN2 function.
 */
public class Atan2Function extends BiCallExpression<Double> {
    public Atan2Function() {
        // No-op.
    }

    private Atan2Function(Expression<?> first, Expression<?> second) {
        super(first, second);
    }

    public static Atan2Function create(Expression<?> first, Expression<?> second) {
        if (!first.getType().canConvertToNumber()) {
            throw HazelcastSqlException.error("Operand 1 is not numeric: " + first.getType());
        }

        if (!second.getType().canConvertToNumber()) {
            throw HazelcastSqlException.error("Operand 2 is not numeric: " + second.getType());
        }

        return new Atan2Function(first, second);
    }

    @Override
    public Double eval(Row row) {
        Double a = operand1.evalAsDouble(row);
        Double b = operand1.evalAsDouble(row);

        if (a == null || b == null) {
            return null;
        }

        return Math.atan2(a, b);
    }

    @Override
    public QueryDataType getType() {
        return QueryDataType.DOUBLE;
    }
}
