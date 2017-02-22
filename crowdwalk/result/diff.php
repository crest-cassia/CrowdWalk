<?php
main($argv[1], $argv[2]);

function main($f1, $f2) {
	$file1 = explode("\n", file_get_contents(getcwd() . '\\' . $f1));
	$file2 = explode("\n", file_get_contents(getcwd() . '\\' . $f2));
	
	$file1_count = count($file1);
	$file2_count = count($file2);
	$length = max(array($file1_count, $file2_count));
	
	if($file1_count !== $file2_count) {
		die('error: file difference is too big.'); 
	}
	
	for($i = 0; $i < $length; $i++) {
		compare($file1[$i], $file2[$i]);
	}
}

function compare($line1, $line2) {
	if($line1 === '' && $line2 === '') {
		return;
	}
	//pedestrianID,current_time,current_position_in_model_x,current_position_in_model_y,current_position_in_model_z,current_linkID
	$col1 = explode(',', $line1);
	$col2 = explode(',', $line2);
	
	$col1_count = count($col1);
	$col2_count = count($col2);
	$length = max($col1_count, $col2_count);
	
	if($col1_count !== $col2_count) {
		echo $line1 . ' and ' . $line2 . ' has different number of columns.' . PHP_EOL;
		return;
	}
	
	//pedestrianID
	if($col1[0] !== $col2[0]) {
		echo $line1 . ' and ' . $line2 . ' has different pedestrianID.' . PHP_EOL;
		return;
	}
	//current_time
	if($col1[1] !== $col2[1]) {
		echo $line1 . ' and ' . $line2 . ' has different current_time.' . PHP_EOL;
		return;
	}
	//current_linkID
	if($col1[5] !== $col2[5]) {
		echo $line1 . ' and ' . $line2 . ' has different current_linkID.' . PHP_EOL;
		return;
	}
	
	//current_position_in_model_x
	if($col1[2] !== $col2[2]) {
		$log = $col1[0] . ',';
		$log .= $col1[1] . ',';
		$log .= 'current_position_in_model_x' . ',';
		$log .= $col1[2] . ',';
		$log .= $col2[2] . ',';
		$log .= compare_value($col1[2], $col2[2]);
		
		echo $log . PHP_EOL;
	}
	
	//current_position_in_model_z
	if($col1[3] !== $col2[3]) {
		$log = $col1[0] . ',';
		$log .= $col1[1] . ',';
		$log .= 'current_position_in_model_x' . ',';
		$log .= $col1[3] . ',';
		$log .= $col2[3] . ',';
		$log .= compare_value($col1[3], $col2[3]);
		
		echo $log . PHP_EOL;
	}
	//current_position_in_model_z
	if($col1[4] !== $col2[4]) {
		$log = $col1[0] . ',';
		$log .= $col1[1] . ',';
		$log .= 'current_position_in_model_x' . ',';
		$log .= $col1[4] . ',';
		$log .= $col2[4] . ',';
		$log .= compare_value($col1[4], $col2[4]);
		
		echo $log . PHP_EOL;
	}
}

function compare_value($v1, $v2) {
	$value1 = explode('.', $v1);
	$value2 = explode('.', $v2);
	
	$length1 = strlen($value1[1]);
	$length2 = strlen($value2[1]);
	$length = max(array($length1, $length2));
	$decimal1 = str_pad($value1[1], $length, '0', STR_PAD_RIGHT);
	$decimal2 = str_pad($value2[1], $length, '0', STR_PAD_RIGHT);
	
	$difference = rtrim(bcsub($v1, $v2, 20), '0');
	
	if($value1[0] !== $value2[0]) {
		return 'integer,' . $difference;
	}
	
	for($i = 0; $i < $length; $i++) {
		if($decimal1[$i] !== $decimal2[$i]) {
			return ($i + 1) . ',' . $difference;
		}
	}
}
?>
