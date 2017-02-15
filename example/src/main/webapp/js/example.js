
$(document).ready(function() {
    $("#swagger-button").click(function(){
        getSwagger();
    });
    $("#version-button").click(function(){
        getVersion();
    });
});

function getSwagger() {
    $.ajax({
        type: "GET",
        url: "api/swagger.json",
        success: function (data) {
            $('#swagger-content').val(JSON.stringify(data, null, 4));
        }
    });
}

function getVersion() {
    $.ajax({
        type: "GET",
        url: "api/version",
        success: function (data) {
            $('#version-content').val(JSON.stringify(data, null, 4));
        }
    });
}