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

package com.hazelcast.sql.impl.expression.predicate;

import com.hazelcast.sql.impl.expression.Expression;
import com.hazelcast.sql.impl.expression.UniCallExpression;
import com.hazelcast.sql.impl.row.Row;
import com.hazelcast.sql.impl.type.QueryDataType;

public class IsFalsePredicate extends UniCallExpression<Boolean> {
    public IsFalsePredicate() {
        // No-op.
    }

    private IsFalsePredicate(Expression<?> operand) {
        super(operand);
    }

    public static IsFalsePredicate create(Expression<?> operand) {
        operand.ensureCanConvertToBit();

        return new IsFalsePredicate(operand);
    }

    @Override
    public Boolean eval(Row row) {
        return PredicateExpressionUtils.isFalse(operand.evalAsBit(row));
    }

    @Override
    public QueryDataType getType() {
        return QueryDataType.BIT;
    }
}
