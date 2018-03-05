var app = angular.module('myApp', ["ngResource","ngRoute","ngCookies","angular-jwt"]);
app.controller('mainCtrl', function($scope,$resource,$http,$httpParamSerializer,$cookies,jwtHelper,$timeout) {
    $scope.foo = {id:1 , name:"sample foo"};
    $scope.foos = $resource("http://localhost:8082/spring-security-oauth-resource/foos/:fooId",{fooId:'@id'});
    
    $scope.organiztion = "";
    $scope.isLoggedIn = false;
    
    $scope.getFoo = function(){
        $scope.foo = $scope.foos.get({fooId:$scope.foo.id});
    }
    
    $scope.loginData = {grant_type:"password", username: "", password: "", client_id: "fooClientIdPassword"};
    $scope.refreshData = {grant_type:"refresh_token"};
        
    var isLoginPage = window.location.href.indexOf("login") != -1;
    if(isLoginPage){
        console.log("is login page");
        if($cookies.get("access_token")){
            window.location.href = "index";
        }
    }else{
        if($cookies.get("access_token")){
            console.log("there is access token");
            $http.defaults.headers.common.Authorization= 'Bearer ' + $cookies.get("access_token");
            getOrganization();
            $scope.isLoggedIn = true;
        }else{
            //obtainAccessToken($scope.refreshData);
            console.log("there is noooo access token");
            $scope.isLoggedIn = false;
            window.location.href = "login";
        }
    }
    
    $scope.login = function() {   
         obtainAccessToken($scope.loginData);
    }
    
    $scope.refreshAccessToken = function(){
        obtainAccessToken($scope.refreshData);
    }
    
    $scope.logout = function() {
        logout($scope.loginData);
    }
    
    if ($cookies.get("remember")=="yes"){
        var validity = $cookies.get("validity");
        if (validity >10) validity -= 10;
        $timeout( function(){;$scope.refreshAccessToken();}, validity * 1000);
    }
    
    function obtainAccessToken(params){
        if (params.username != null){
            if (params.remember != null){
                $cookies.put("remember","yes");
            }
            else {
                $cookies.remove("remember");
            }
        }
    
        var req = {
            method: 'POST',
            url: "oauth/token",
            headers: {"Content-type": "application/x-www-form-urlencoded; charset=utf-8"},
            data: $httpParamSerializer(params)
        }
        $http(req).then(
            function(data){
                $http.defaults.headers.common.Authorization= 'Bearer ' + data.data.access_token;
                var expireDate = new Date (new Date().getTime() + (1000 * data.data.expires_in));
                $cookies.put("access_token", data.data.access_token, {'expires': expireDate});
                $cookies.put("validity", data.data.expires_in);
                window.location.href="index";
            },function(){
                console.log("error");
                window.location.href = "login";
            }
        );
    }
    
    function getOrganization(){
        var token = $cookies.get("access_token");
        //JWT
        /* var payload = jwtHelper.decodeToken(token);
        console.log(payload);
        $scope.organization = payload.organization; */
        
        //JDBC
         $http.get("http://localhost:8082/spring-security-oauth-resource/users/extra")
        .then(function(response) {
            console.log(response);
            $scope.organization = response.data.organization;
        }); 
    }
    
    function logout(params) {
        var req = {
            method: 'DELETE',
            url: "oauth/token"
        }
        $http(req).then(
            function(data){
                $cookies.remove("access_token");
                $cookies.remove("validity");
                $cookies.remove("remember");
                window.location.href="login";
            },function(){
                console.log("error");
            }
        );
    }
    
});
app.factory('rememberMeInterceptor', ['$q','$injector','$httpParamSerializer', function($q, $injector,$httpParamSerializer) {  
    var interceptor = {
        responseError: function(response) {
            if (response.status == 401){
                
                var $http = $injector.get('$http');
                var $cookies = $injector.get('$cookies');
                var deferred = $q.defer();
                var refreshData = {grant_type:"refresh_token"};
                
                var req = {
                    method: 'POST',
                    url: "oauth/token",
                    headers: {"Content-type": "application/x-www-form-urlencoded; charset=utf-8"},
                    data: $httpParamSerializer(refreshData)
                }
    
                $http(req).then(
                    function(data){
                        $http.defaults.headers.common.Authorization= 'Bearer ' + data.data.access_token;
                        var expireDate = new Date (new Date().getTime() + (1000 * data.data.expires_in));
                        $cookies.put("access_token", data.data.access_token, {'expires': expireDate});
                        $cookies.put("validity", data.data.expires_in);
                        window.location.href="index";
                    },function(){
                        console.log("error");
                        $cookies.remove("access_token");
                        window.location.href = "login";
                    }
                );
                // make the backend call again and chain the request
                return deferred.promise.then(function() {
                    return $http(response.config);
                });
            }
            return $q.reject(response);
        }
    };
    return interceptor;
}]);
app.config(['$httpProvider', function($httpProvider) {  
    $httpProvider.interceptors.push('rememberMeInterceptor');
}]);