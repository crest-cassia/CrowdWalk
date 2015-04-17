require 'rexml/document'


class MapfileOperator
	
	def initialize xmlFilename
		@xmlFilename = xmlFilename	

		@document = REXML::Document.new(open(@xmlFilename))
	end
	
	# 変更を保存
	def save filename
		if filename==nil then
			filename = @xmlFilename
		end
		File.open(filename, "w") do |io|
			io.puts @document
		end
	end

	# 現在のXMLを表示する
	def print
		puts @document
	end

	# タグが付いたリンクを取得する
	def getLinksByTag tag_name
		array = Array.new
		#count = 0
		@document.elements.each('Group/Group/Link') do |child|
			#count += 1
			
			class << child
				# リンクを追加する
				def addTag tag_name
					if !self.hasTag?(tag_name) then
						element = REXML::Element.new("tag")
						element.add_text(tag_name)
							
						# タグを追加		
						self.add_element(element)
					end
				end

				# タグがリンクをもっていればtrue, もっていなければfalse
				def hasTag? tag_name
					flag = false

					self.elements.each do |child|
						if child.text==tag_name then
							flag = true
							break	
						end
					end
					
					return flag
				end
			end	

			if child.hasTag?(tag_name) then
				array.push child
			end
		end
		#puts count

		return array
	end
	
	# すべてのリンクを取得する
	def getAllLinks 
		array = Array.new
		@document.elements.each('Group/Group/Link') do |child|
				
			class << child
				# リンクを追加する
				def addTag tag_name
					if !self.hasTag?(tag_name) then
						element = REXML::Element.new("tag")
						element.add_text(tag_name)
							
						# タグを追加		
						self.add_element(element)
					end
				end

				# タグがリンクをもっていればtrue, もっていなければfalse
				def hasTag? tag_name
					flag = false

					self.elements.each do |child|
						if child.text==tag_name then
							flag = true
							break	
						end
					end
					
					return flag
				end

			end	

			array.push child
		end

		return array
	end

end



