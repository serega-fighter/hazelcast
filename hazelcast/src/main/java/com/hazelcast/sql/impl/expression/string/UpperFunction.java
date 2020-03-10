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

package com.hazelcast.sql.impl.expression.string;

import com.hazelcast.sql.impl.expression.Expression;
import com.hazelcast.sql.impl.expression.UniCallExpression;
import com.hazelcast.sql.impl.row.Row;
import com.hazelcast.sql.impl.type.QueryDataType;

public class UpperFunction extends UniCallExpression<String> {
    public UpperFunction() {
        // No-op.
    }

    private UpperFunction(Expression<?> operand) {
        super(operand);
    }

    public static UpperFunction create(Expression<?> operand) {
        operand.ensureCanConvertToVarchar();

        return new UpperFunction(operand);
    }

    @Override
    public String eval(Row row) {
        String value = operand.evalAsVarchar(row);

        return StringExpressionUtils.upper(value);
    }

    @Override
    public QueryDataType getType() {
        return QueryDataType.VARCHAR;
    }
}
