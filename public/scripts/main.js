"use strict";
angular.module("app", ["chart.js"]).controller("LineCtrl", function ($scope, $http) {

	$http.get('/data')
		.success(function(data) {
			var k = [];
			var RMSE = [];

			for (var i = 0; i < data.length; i++) {
				k.push(data[i].k);
				RMSE.push(data[i].RMSE);
			};

			$scope.labels = k;
			$scope.data = RMSE;
		})
		.error(function(resp) {
			console.log('error server');
		});


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