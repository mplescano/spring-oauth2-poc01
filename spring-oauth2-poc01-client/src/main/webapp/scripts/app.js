var app = angular.module('myApp', [ "ngResource", "ngRoute", "oauth" ]);
app.config(function($locationProvider) {
	$locationProvider.html5Mode({
		enabled : true,
		requireBase : false
	}).hashPrefix('!');
});
app.config([ '$httpProvider', function($httpProvider) {
	$httpProvider.interceptors.push(function($q, $rootScope) {
		return {
			'responseError' : function(responseError) {
				$rootScope.message = responseError.statusText;
				console.log("error here");
				console.log(responseError);
				return $q.reject(responseError);
			}
		};
	});
} ]);

app.controller('mainCtrl', function($scope, $resource, $http, $rootScope) {
					$scope.$on('oauth:login', function(event, token) {
						$http.defaults.headers.common.Authorization = 'Bearer ' + token.access_token;
						console.log('Authorized third party app with token', token.access_token);
						$scope.token = token.access_token;
					});
					$scope.foo = {
						id : 0,
						name : "sample foo"
					};
					$scope.foos = $resource("http://localhost:8090/foos/:fooId",
							{
								fooId : '@id'
							});
					$scope.getFoo = function() {
						$scope.foo = $scope.foos.get({
							fooId : $scope.foo.id
						});
					}
					$scope.createFoo = function() {
						if ($scope.foo.name.length == 0) {
							$rootScope.message = "Foo name can not be empty";
							return;
						}
						$scope.foo.id = null;
						$scope.foo = $scope.foos.save($scope.foo, function() {
							$rootScope.message = "Foo Created Successfully";
						});
					}

					$scope.revokeToken = $resource("http://localhost:8080/oauth/token/revokeById/:tokenId",
							{
								tokenId : '@tokenId'
							});
					$scope.tokens = $resource("http://localhost:8080/tokens");
					$scope.getTokens = function() {
						$scope.tokenList = $scope.tokens.query();
					}
					$scope.revokeAccessToken = function() {
						if ($scope.tokenToRevoke && $scope.tokenToRevoke.length != 0) {
							$scope.revokeToken.save({
								tokenId : $scope.tokenToRevoke
							});
							$rootScope.message = "Token:" + $scope.tokenToRevoke + " was revoked!";
							$scope.tokenToRevoke = "";
						}
					}
				});