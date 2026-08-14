package pt.uninova.ditag.tool.utils;

import pt.uninova.ditag.tool.node.SchemaNode.DataType;

public final class EvalExConverter {

    private EvalExConverter() {}

    public static String convertFromString(DataType dataType, String expressionValue) {
        String resultValue;

        switch (dataType) {
            case STRING:
            case NORMALIZED_STRING:
                resultValue = expressionValue;
                break;
            case FLOAT:
                resultValue = "Float.parseFloat(" + expressionValue + ")";
                break;
            case DOUBLE:
                resultValue = "Double.parseDouble(" + expressionValue + ")";
                break;
            case LONG:
                resultValue = "Long.parseLong(" + expressionValue + ")";
                break;
            case INT:
                resultValue = "Integer.parseInt(" + expressionValue + ")";
                break;
            case SHORT:
                resultValue = "Short.parseShort(" + expressionValue + ")";
                break;
            case BYTE:
                resultValue = "Byte.parseByte(" + expressionValue + ")";
                break;
            case U_LONG:
                resultValue = "Long.parseUnsignedLong(" + expressionValue + ")";
                break;
            case U_INT:
                resultValue = "Integer.parseUnsignedInt(" + expressionValue + ")";
                break;
            case U_SHORT:
            case U_BYTE:
                resultValue = "Integer.parseInt(" + expressionValue + ")";
                break;
            case NON_NEG_INTEGER:
            case POS_INTEGER:
            case NON_POS_INTEGER:
            case NEG_INTEGER:
                resultValue = "Integer.parseInt(" + expressionValue + ")";
                break;
            case DATE:
                resultValue = "java.time.LocalDate.parse(" + expressionValue + ")";
                break;
            case TIME:
                resultValue = "java.time.LocalTime.parse(" + expressionValue + ")";
                break;
            case DATE_TIME:
                resultValue = "java.time.LocalDateTime.parse(" + expressionValue + ")";
                break;
            case OTHER:
            case NONE:
            default:
                resultValue = expressionValue;
                break;
        }

        return resultValue;
    }
}