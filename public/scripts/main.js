"use strict";
angular.module("app", ["chart.js"]).controller("LineCtrl", function ($scope, $http) {

	$http.get('/data')
		.success(function(data) {
			$scope.labels = data.k;
			$scope.data = [data.RMSE, data.RMSE];
		})
		.error(function(resp) {
			console.log('error server');
		});

	$scope.submit = function() {
		console.log($scope.film);
		$http.post('/handerfilm', {
			filmName: $scope.film
		}).success(function(data) {
			console.log(data);
		}).error(function(res) {
			console.log(res);
		});
		console.log("submit");
	};

	// $http.get('/tuyen')
	// 	.success(function(data) {
	// 		$scope.labels = data.RMSE;
	// 		$scope.data = [data.k, data.k];
	// 	});

  // $scope.labels = ["January", "February", "March", "April", "May", "June", "July"];
  // $scope.series = ['Series A'];
  // $scope.data = [
  //   [65, 59, 80, 81, 56, 55, 40],
  //   [28, 48, 40, 19, 86, 27, 90]
  // ];
  $scope.onClick = function (points, evt) {
    console.log(points, evt);
  };
});