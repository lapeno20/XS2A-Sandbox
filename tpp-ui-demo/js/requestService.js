function sendPaymentRequestAndGetResponse(productNumber) {
    var paymentResponse = {};

    var settings = getPaymentAjaxSettings(productNumber);
    $.ajax(settings)
        .done(function (resp) {
            console.log("complete : " + JSON.stringify(resp));
            paymentResponse = resp;

        })
        .fail(function (e) {
            console.log("ERROR: ", e);
            alert("Connection error!");
        });

    return paymentResponse;
}

function getPaymentAjaxSettings(productNumber) {
    var paymentReqJson = getPaymentInitiationRequestJson(productNumber);
    var headers = getRequestHeaders();
    var xs2aUrl = configs.localhost;

    return {
        "async": false,
        "crossDomain": true,
        "url": xs2aUrl,
        "method": "POST",
        "headers": headers,
        "processData": false,
        "data": paymentReqJson
    };

}

function getPaymentInitiationRequestJson(productNumber) {
    var formObject = {};

    var debtorAccount = {};
    var creditorAccount = {};
    var instructedAmount = {};

    debtorAccount.iban = $("#debtorIban").val();
    debtorAccount.currency = $("#debtorCurrency").val();

    creditorAccount.iban = $("#creditorIban").val();
    creditorAccount.currency = $("#creditorCurrency").val();

    instructedAmount.amount = $("#amount" + productNumber).val();
    instructedAmount.currency = $("#currency" + productNumber).val();

    formObject.debtorAccount = debtorAccount;
    formObject.creditorAccount = creditorAccount;
    formObject.instructedAmount = instructedAmount;

    formObject.ultimateDebtor = $("#debtorName").val();
    formObject.creditorName = $("#creditorName").val();
    formObject.ultimateCreditor = $("#creditorName").val();
    formObject.remittanceInformationUnstructured = $("#productName"+ productNumber).val();

    var dateTime = new Date();
    dateTime.setDate(dateTime.getDate() + 1);
    var formattedDateTimeString = dateTime.toISOString();

    var formattedDate = formattedDateTimeString.substring(10, 0);
    var formattedDateTime = formattedDateTimeString.substring(23, 0);

    formObject.requestedExecutionDate = formattedDate;

    return JSON.stringify(formObject);
}

function getRequestHeaders() {

    var headers = {};

    headers["PSU-ID"] = "anton.brueckner";
    headers["TPP-Explicit-Authorisation-Preferred"] = "false";
    headers["TPP-Redirect-Preferred"] = "true";
    headers["TPP-Redirect-URI"] = "https://google.com";
    headers["tpp-transaction-id"] = "16d40f49-a110-4344-a949-f99828ae13c9";
    headers["X-Request-ID"] = "703d6582-334b-45d2-8fdb-1854d1e35c7d";
    headers["PSU-IP-Address"] = "95.67.106.182";
    headers["content-type"] = "application/json";

    return headers;
}
