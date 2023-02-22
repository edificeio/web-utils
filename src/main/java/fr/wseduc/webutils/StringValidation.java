package fr.wseduc.webutils;

public final class StringValidation {

    private StringValidation() {}

    public static String obfuscateMobile(String mobile){
        return mobile.length() > 4 ?
                mobile.substring(0, mobile.length() - 4).replaceAll(".", ".") +
                        mobile.substring(mobile.length() - 4) : mobile.replaceAll(".", ".");
    }

    public static String formatPhone(String phone){
        final String formattedPhone = phone.replaceAll("[^0-9\\\\+]", "");
        return !formattedPhone.startsWith("00") && !formattedPhone.startsWith("+") && formattedPhone.startsWith("0") && formattedPhone.length() == 10 ?
                formattedPhone.replaceFirst("0", "+33") :
                formattedPhone;
    }
}
