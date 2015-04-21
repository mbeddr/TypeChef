package com.mbeddr.core.importer;

import java.util.List;

public interface PartialCodeChecker {

    public boolean canParseExpression(String code);
    public boolean canParseStatement(String code);

}
