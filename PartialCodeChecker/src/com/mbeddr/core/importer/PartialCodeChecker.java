package com.mbeddr.core.importer;

public interface PartialCodeChecker {

    public boolean canParseExpression(String code);
    public boolean canParseStatement(String code);

}
