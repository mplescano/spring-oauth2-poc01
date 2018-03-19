var app = angular.module('myApp', ["ngResource","ngRoute","ngCookies","angular-jwt"]);
app.config(['$routeProvider', '$httpProvider', function($routeProvider, $httpProvider) {
	
    $routeProvider
    .when('/session',{
    	controller: 'mainSessionCtrl',
        templateUrl: 'explicit-session.html',
        controllerAs: '$ctrl'
    })
    .when('/login', {
    	controller: 'mainLoginCtrl',
    	templateUrl: 'explicit-login.html',
        controllerAs: '$ctrl'
    })
    .otherwise({
    	template: '<h1>Root</h1>' 
    });
}]);
/*app.config(['$httpProvider', function($httpProvider) {
    $httpProvider.interceptors.push('rememberMeInterceptor');
}]);*/

app.controller('mainLoginCtrl', mainLoginCtrl);
mainLoginCtrl.$inject = ['$scope','$rootScope','$resource','$http','$httpParamSerializer','$cookies','jwtHelper','$timeout','$location'];
function mainLoginCtrl($scope,$rootScope,$resource,$http,$httpParamSerializer,$cookies,jwtHelper,$timeout,$location) {
    $rootScope.organization = "";
    $rootScope.isLoggedIn = false;

    $rootScope.loginData = {grant_type:"password", username: "", password: ""};//, client_id: "fooClientIdPassword"
    $scope.login = function() {
         obtainAccessToken($rootScope.loginData);
    }
    
    if($cookies.get("access_token")){
    	$location.path('/session');
    }
    else {
    	delete $http.defaults.headers.common["Authorization"];
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
            url: "http://" + GLB_HOSTNAME + ":8070/proxy/oauth/token",
            headers: {
                "Content-type": "application/x-www-form-urlencoded; charset=utf-8"
            },
            data: $httpParamSerializer(params)
        };
        $http(req).then(
            function(data){
                $http.defaults.headers.common.Authorization= 'Bearer ' + data.data.access_token;
                var expireDate = new Date (new Date().getTime() + (1000 * data.data.expires_in));
                $cookies.put("access_token", data.data.access_token, {'expires': expireDate});
                $cookies.put("validity", data.data.expires_in);
                $cookies.put("refresh_token", data.data.refresh_token);
                $rootScope.isLoggedIn = true;
                $location.path('/session');
            },function(response){
                console.log("error", response);
                $rootScope.isLoggedIn = false;
                //window.location.href = "login";
            }
        );
    }
}

app.controller('mainSessionCtrl', mainSessionCtrl);
mainSessionCtrl.$inject = ['$scope','$rootScope','$resource','$http','$httpParamSerializer','$cookies','jwtHelper','$timeout','$location'];
function mainSessionCtrl($scope,$rootScope,$resource,$http,$httpParamSerializer,$cookies,jwtHelper,$timeout,$location) {
    $scope.foo = {id:1 , name:"sample foo"};
    $scope.foos = $resource("http://" + GLB_HOSTNAME + ":8090/foos/:fooId", {fooId:'@id'});

    $scope.getFoo = function(){
        $scope.foo = $scope.foos.get({fooId:$scope.foo.id});
    }
    $rootScope.refreshData = {grant_type:"refresh_token", refresh_token:$cookies.get("refresh_token")};
    $scope.refreshAccessToken = function() {
        obtainNewAccessToken($rootScope.refreshData);
    }
    
    if($cookies.get("access_token")){
        console.log("there is access token");
        $http.defaults.headers.common.Authorization = 'Bearer ' + $cookies.get("access_token");
        getOrganization();
        $rootScope.isLoggedIn = true;
    }
    else {
        console.log("there is noooo access token");
        $rootScope.isLoggedIn = false;
        delete $http.defaults.headers.common["Authorization"];
        $location.path('/login');
    }
    
    $scope.logout = function() {
        logout($rootScope.loginData);
    }
    
    function logout(params) {
        var req = {
            method: 'DELETE',
            headers: {
                "Authorization": 'Bearer ' + $cookies.get("access_token")
            },
            url: "http://" + GLB_HOSTNAME + ":8070/proxy/oauth/token"
        }
        $http(req).then(
            function(data){
                $cookies.remove("access_token");
                $cookies.remove("refresh_token");
                $cookies.remove("validity");
                $cookies.remove("remember");
                $location.path('/login');
                //window.location.href="login";
            },function(response){
                console.log("error", response);
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
         $http.get("http://" + GLB_HOSTNAME + ":8090/users/extra")
        .then(function(response) {
            console.log(response);
            $rootScope.organization = response.data.organization;
        }); 
    }
    
    function obtainNewAccessToken(params) {
        var req = {
            method: 'POST',
            url: "http://" + GLB_HOSTNAME + ":8070/proxy/oauth/token",
            headers: {
                "Content-type": "application/x-www-form-urlencoded; charset=utf-8"
            },
            data: $httpParamSerializer(params)
        };
        $http(req).then(
            function(data){
                $http.defaults.headers.common.Authorization= 'Bearer ' + data.data.access_token;
                var expireDate = new Date (new Date().getTime() + (1000 * data.data.expires_in));
                $cookies.put("access_token", data.data.access_token, {'expires': expireDate});
                $cookies.put("refresh_token", data.data.refresh_token);
                $cookies.put("validity", data.data.expires_in);
                $rootScope.isLoggedIn = true;
            },function(response){
                console.log("error", response);
                $rootScope.isLoggedIn = false;
                $location.path('/login');
                //window.location.href = "login";
            }
        );
    }
}
