"use strict";
angular.module("app", ["chart.js"]).controller("LineCtrl", function ($scope, $http) {

	$scope.check = true;

	$http.get('/data')
		.success(function(data) {
			$scope.labels = data.k;
			$scope.data = [data.RMSE, data.RMSE];
			$scope.check = false;
		})
		.error(function(resp) {
			console.log('error server');
		});

	$scope.submit = function() {
		$http.post('/searchfilm', {
			iduser: $scope.iduser
		}).success(function(data) {
			// console.log(data);
			$scope.user = 'gợi ý cho người dùng ' +  data.userid;
			var films = data.films;
			$scope.items = [];

			for (var i = 0; i < 10; i++) {
				$scope.items.push(films[i]);
			};
		}).error(function(res) {
			console.log(res);
		});

	};

});